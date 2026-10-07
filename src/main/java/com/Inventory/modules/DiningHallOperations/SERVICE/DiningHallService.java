package com.Inventory.modules.DiningHallOperations.SERVICE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory.modules.DiningHallOperations.DAO.DiningHallDAO;
import com.Inventory.SESSION.RoleAccess;

@Service
public class DiningHallService {
    public static final List<String> SESSIONS=List.of("Morning Drink","Break Fast","Lunch","Snacks","Staff Tea","Dinner","Special Event","Adjustment");
    private final DiningHallDAO dao;
    public DiningHallService(DiningHallDAO dao){this.dao=dao;}

    public Map<String,Object> entryMasterData(){return dao.entryMasterData();}
    public String previewIssueNo(){return dao.previewIssueNumber();}
    public Map<String,Object> dashboardMetrics(LocalDate date){return dao.dashboardMetrics(date);}
    public List<Map<String,Object>> dailyDashboard(LocalDate from,LocalDate to){return dao.dailyDashboard(from,to);}
    public List<Map<String,Object>> sessionDashboard(){return dao.sessionDashboard();}
    public List<Map<String,Object>> consumptionReport(String month,String session){
        YearMonth ym=month(month);return dao.consumptionReport(ym.atDay(1),ym.atEndOfMonth(),session);
    }
    public List<Map<String,Object>> consumptionOnDate(String date){return dao.consumptionOnDate(parseDate(date,"Selected date"));}

    public List<List<Map<String,Object>>> buildDiningCalendar(YearMonth month, List<Map<String,Object>> rows) {
        Map<LocalDate, Map<String, List<Map<String,Object>>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate date = ((java.sql.Date) row.get("date")).toLocalDate();
            grouped.computeIfAbsent(date, d -> new LinkedHashMap<>())
                   .computeIfAbsent(String.valueOf(row.get("session")), s -> new ArrayList<>()).add(row);
        }

        List<List<Map<String,Object>>> weeks = new ArrayList<>();
        List<Map<String,Object>> week = new ArrayList<>();
        int leading = month.atDay(1).getDayOfWeek().getValue() % 7; // Sunday = 0, Mon = 1, etc.
        for (int i = 0; i < leading; i++) week.add(Map.of("empty", true));

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            Map<String, Object> dayCell = new LinkedHashMap<>();
            dayCell.put("empty", false);
            dayCell.put("day", day);
            dayCell.put("date", date);

            List<Map<String, Object>> sessionCells = new ArrayList<>();
            BigDecimal dayTotal = BigDecimal.ZERO;
            Map<String, List<Map<String, Object>>> sessionItems = grouped.getOrDefault(date, Map.of());

            for (String session : SESSIONS) {
                List<Map<String, Object>> items = sessionItems.get(session);
                if (items == null || items.isEmpty()) continue;

                BigDecimal value = items.stream()
                        .map(item -> (BigDecimal) item.get("value"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id", sessionCells.size() + 1 + day * 10);
                s.put("name", session);
                s.put("value", value);
                s.put("items", items);
                s.put("color", session.toLowerCase().replace(' ', '-'));
                sessionCells.add(s);
                dayTotal = dayTotal.add(value);
            }

            dayCell.put("sessions", sessionCells);
            dayCell.put("total", dayTotal);
            week.add(dayCell);

            if (week.size() == 7) {
                weeks.add(week);
                week = new ArrayList<>();
            }
        }
        while (!week.isEmpty() && week.size() < 7) week.add(Map.of("empty", true));
        if (!week.isEmpty()) weeks.add(week);

        return weeks;
    }

    @Transactional
    public String create(String recipient,String mealSession,String dateValue,int[] itemIds,String[] quantities,String[] remarks,String role){
        String name=required(recipient,"Issued to");String session=required(mealSession,"Session");
        if(!SESSIONS.contains(session))throw new IllegalArgumentException("Select a valid dining session.");
        if("Adjustment".equals(session)&&!RoleAccess.isGlobal(role))throw new IllegalArgumentException("Only Global or Super Admin can use the Adjustment session.");
        LocalDate date=parseDate(dateValue,"Issue date");
        if(itemIds==null||quantities==null||itemIds.length==0||itemIds.length!=quantities.length||(remarks!=null&&remarks.length!=itemIds.length))
            throw new IllegalArgumentException("Add at least one complete item row.");
        String issueNo=dao.allocateIssueNumber();Set<Integer> seen=new HashSet<>();int saved=0;
        for(int i=0;i<itemIds.length;i++){
            if(itemIds[i]<=0)throw new IllegalArgumentException("Select an item in every row.");
            if(!seen.add(itemIds[i]))throw new IllegalArgumentException("Keep one row per item; duplicate items are not allowed.");
            BigDecimal qty=quantity(quantities[i],"Quantity");if(qty.signum()<=0)throw new IllegalArgumentException("Quantity must be greater than zero.");
            Map<String,Object> stock=dao.lockStock(itemIds[i]);
            if(stock==null)throw new IllegalArgumentException("No stock record exists for selected item ID "+itemIds[i]+".");
            BigDecimal available=(BigDecimal)stock.get("balance");
            if(available.signum()<0)throw new IllegalArgumentException("Stock is already negative for item ID "+itemIds[i]+".");
            if(qty.compareTo(available)>0)throw new IllegalArgumentException("Insufficient stock for item ID "+itemIds[i]+". Available: "+available.toPlainString()+".");
            BigDecimal price=dao.latestUnitPrice(itemIds[i],(BigDecimal)stock.get("lastPrice"));
            BigDecimal total=qty.multiply(price).setScale(2,RoundingMode.HALF_UP);
            String remark=remarks==null?null:trimToNull(remarks[i]);
            Object poItemId=stock.get("poItemId");
            int consumptionId=dao.insertConsumption(issueNo,itemIds[i],poItemId,name,session,qty,remark,price,total,date);
            int stockIssueId=dao.insertStockIssue(issueNo,itemIds[i],name,qty,remark,price,total,date);
            dao.applyIssueToStock((Integer)stock.get("id"),qty,price);
            BigDecimal newBalance=available.subtract(qty);
            dao.insertLedger(consumptionId,itemIds[i],stockIssueId,date,qty,newBalance,remark);
            dao.reconcileStock(itemIds[i]);saved++;
        }
        if(saved==0)throw new IllegalArgumentException("Add at least one item with a quantity.");
        return issueNo;
    }

    @Transactional
    public int update(LocalDate date,List<Integer> selectedIds,Map<String,List<String>> params){
        if(selectedIds==null||selectedIds.isEmpty())throw new IllegalArgumentException("Select at least one consumption row to update.");
        int updated=0;Set<Integer> unique=new HashSet<>();
        for(Integer id:selectedIds){
            if(id==null||!unique.add(id))continue;
            Map<String,Object> old=dao.lockConsumption(id,date);
            if(old==null)throw new IllegalArgumentException("A selected consumption row was not found for this date.");
            BigDecimal qty=quantity(first(params,"qty_issued_"+id),"Quantity");
            BigDecimal prior=(BigDecimal)old.get("qty");BigDecimal difference=qty.subtract(prior);
            if(difference.signum()>0){BigDecimal available=dao.lockCurrentBalance((Integer)old.get("itemId"));
                if(difference.compareTo(available)>0)throw new IllegalArgumentException("Updated quantity exceeds available stock for "+old.get("itemId")+".");}
            String recipient=required(first(params,"issued_to_"+id),"Issued to");
            String remarks=trimToNull(first(params,"remarks_"+id));
            dao.updateConsumption(id,(String)old.get("issueNo"),(Integer)old.get("itemId"),qty,(BigDecimal)old.get("price"),difference,"Dining Hall",recipient,remarks);
            updated++;
        }
        return updated;
    }

    private YearMonth month(String raw){
        try{return YearMonth.parse(required(raw,"Report month"));}
        catch(DateTimeParseException e){throw new IllegalArgumentException("Select a valid report month.");}
    }
    private LocalDate parseDate(String raw,String label){
        try{return LocalDate.parse(required(raw,label));}
        catch(DateTimeParseException e){throw new IllegalArgumentException("Enter a valid "+label.toLowerCase()+".");}
    }
    private BigDecimal quantity(String raw,String label){
        try{BigDecimal n=new BigDecimal(required(raw,label).trim());if(n.scale()>4||n.signum()<0)throw new NumberFormatException();return n;}
        catch(NumberFormatException e){throw new IllegalArgumentException(label+" must be a non-negative number with up to four decimal places.");}
    }
    private String first(Map<String,List<String>> params,String key){List<String> values=params.get(key);return values==null||values.isEmpty()?null:values.get(0);}
    private String required(String value,String label){if(value==null||value.isBlank())throw new IllegalArgumentException(label+" is required.");return value.trim();}
    private String trimToNull(String s){return s==null||s.isBlank()?null:s.trim();}
}