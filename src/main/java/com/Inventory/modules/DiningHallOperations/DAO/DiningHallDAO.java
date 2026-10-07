package com.Inventory.modules.DiningHallOperations.DAO;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DiningHallDAO {
    private final JdbcTemplate jdbc;
    public DiningHallDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> entryMasterData() {
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("categories", jdbc.query("SELECT DISTINCT Category, Department FROM dept_cate WHERE Department=? ORDER BY Category",
                (rs, row) -> { Map<String,Object> m=new LinkedHashMap<>(); m.put("name",rs.getString("Category")); m.put("departmentName",rs.getString("Department")); return m; },"Dining Hall"));
        master.put("subcategories", jdbc.query("SELECT Sub_Category, Category FROM category WHERE Status='Active' ORDER BY Category, Sub_Category",
                (rs, row) -> { Map<String,Object> m=new LinkedHashMap<>(); m.put("name",rs.getString("Sub_Category")); m.put("category",rs.getString("Category")); return m; }));
        master.put("items", jdbc.query("SELECT im.Item_id, im.Item_name, im.UOM, im.Category, im.Sub_Category, "
                        + "COALESCE(s.balance_qty, 0) AS stock FROM item_master im LEFT JOIN stock s ON im.Item_id=s.item_id "
                        + "ORDER BY im.Category,im.Sub_Category,im.Item_name", (rs,row) -> {
                    Map<String,Object> m=new LinkedHashMap<>(); m.put("id",rs.getInt("Item_id")); m.put("name",rs.getString("Item_name"));
                    m.put("uom",rs.getString("UOM")); m.put("category",rs.getString("Category")); m.put("subcategory",rs.getString("Sub_Category"));
                    m.put("stock",nz(rs.getBigDecimal("stock"))); return m;
                }));
        return master;
    }

    public String allocateIssueNumber() {
        Integer next=jdbc.queryForObject("SELECT COALESCE(MAX(CAST(SUBSTRING(issueno,4) AS UNSIGNED)),0)+1 "
                + "FROM dining_hall_consumption",Integer.class);
        return "ISS"+(next==null?1:next);
    }

    public String previewIssueNumber() {
        Integer next=jdbc.queryForObject("SELECT COALESCE(MAX(CAST(SUBSTRING(issueno,4) AS UNSIGNED)),0)+1 "
                + "FROM dining_hall_consumption",Integer.class);
        return "ISS"+(next==null?1:next);
    }

    public Map<String,Object> lockStock(int itemId) {
        List<Map<String,Object>> rows=jdbc.query("SELECT stock_id,po_item_id,balance_qty,last_price FROM stock WHERE item_id=? ORDER BY stock_id FOR UPDATE",
                (rs,row)->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getInt("stock_id"));m.put("poItemId",rs.getObject("po_item_id"));
                    m.put("balance",nz(rs.getBigDecimal("balance_qty")));m.put("lastPrice",nz(rs.getBigDecimal("last_price")));return m;},itemId);
        if(rows.size()>1) throw new IllegalArgumentException("More than one stock record exists for item ID "+itemId+".");
        return rows.isEmpty()?null:rows.get(0);
    }

    public BigDecimal latestUnitPrice(int itemId, BigDecimal fallback) {
        List<BigDecimal> prices=jdbc.query("SELECT net_amount/NULLIF(qty,0) FROM po_items WHERE item_id=? AND qty>0 ORDER BY PO_id DESC,sl_no DESC LIMIT 1",
                (rs,row)->rs.getBigDecimal(1),itemId);
        return prices.isEmpty()||prices.get(0)==null?fallback:prices.get(0);
    }

    public int insertConsumption(String issueNo,int itemId,Object poItemId,String recipient,String mealSession,
                                 BigDecimal quantity,String remarks,BigDecimal price,BigDecimal total,LocalDate issueDate) {
        KeyHolder keys=new GeneratedKeyHolder();
        jdbc.update(connection->{var ps=connection.prepareStatement("INSERT INTO dining_hall_consumption(issueno,item_id,po_item_id,department,issued_to,qty_issued,remarks,unit_price,total_value,session,issue_date) VALUES(?,?,?,'Dining Hall',?,?,?,?,?,?,?)",new String[]{"issue_id"});
            ps.setString(1,issueNo);ps.setInt(2,itemId);ps.setObject(3,poItemId);ps.setString(4,recipient);ps.setBigDecimal(5,quantity);
            ps.setString(6,remarks);ps.setBigDecimal(7,price);ps.setBigDecimal(8,total);ps.setString(9,mealSession);ps.setObject(10,issueDate);return ps;},keys);
        Number key=keys.getKey();if(key==null)throw new IllegalStateException("Consumption record was saved without an ID.");return key.intValue();
    }

    public int insertStockIssue(String issueNo,int itemId,String recipient,BigDecimal quantity,String remarks,BigDecimal price,BigDecimal total,LocalDate date) {
        KeyHolder keys=new GeneratedKeyHolder();
        jdbc.update(connection->{var ps=connection.prepareStatement("INSERT INTO stock_issues(issueno,item_id,department,issued_to,qty_issued,remarks,unit_price,total_value,issue_date) VALUES(?,?,'Dining Hall',?,?,?,?,?,?)",new String[]{"issue_id"});
            ps.setString(1,issueNo);ps.setInt(2,itemId);ps.setString(3,recipient);ps.setBigDecimal(4,quantity);ps.setString(5,remarks);
            ps.setBigDecimal(6,price);ps.setBigDecimal(7,total);ps.setObject(8,date);return ps;},keys);
        Number key=keys.getKey();if(key==null)throw new IllegalStateException("Stock issue record was saved without an ID.");return key.intValue();
    }

    public void applyIssueToStock(int stockId,BigDecimal quantity,BigDecimal price) {
        int updated=jdbc.update("UPDATE stock SET total_issued=COALESCE(total_issued,0)+?,balance_qty=COALESCE(balance_qty,0)-?,last_price=?,last_updated=NOW() WHERE stock_id=? AND balance_qty>=?",
                quantity,quantity,price,stockId,quantity);
        if(updated!=1)throw new IllegalArgumentException("Stock changed while saving. Reload the form and retry.");
    }

    public void insertLedger(int consumptionId,int itemId,int stockIssueId,LocalDate date,BigDecimal qty,BigDecimal balance,String remarks) {
        jdbc.update("INSERT INTO stock_ledger(consumption_id,item_id,trans_type,trans_id,trans_date,qty,running_balance,remarks) VALUES(?,?,'ISSUE',?,?,?,?,?)",
                consumptionId,itemId,stockIssueId,date,qty,balance,remarks);
    }

    public void reconcileStock(int itemId) {
        List<Map<String,Object>> sums=jdbc.query("SELECT COALESCE(SUM(CASE WHEN UPPER(TRIM(trans_type))='RECEIPT' THEN qty ELSE 0 END),0) received,"
                        + "COALESCE(SUM(CASE WHEN UPPER(TRIM(trans_type))='ISSUE' THEN qty ELSE 0 END),0) issued,"
                        + "COALESCE(SUM(CASE WHEN UPPER(TRIM(trans_type))='RECEIPT' THEN qty WHEN UPPER(TRIM(trans_type))='ISSUE' THEN -qty ELSE 0 END),0) balance "
                        + "FROM stock_ledger WHERE item_id=?",(rs,row)->{Map<String,Object> m=new LinkedHashMap<>();m.put("received",nz(rs.getBigDecimal("received")));m.put("issued",nz(rs.getBigDecimal("issued")));m.put("balance",nz(rs.getBigDecimal("balance")));return m;},itemId);
        Map<String,Object> sum=sums.get(0);
        jdbc.update("UPDATE stock SET total_received=?,total_issued=?,balance_qty=?,last_updated=NOW() WHERE item_id=?",sum.get("received"),sum.get("issued"),sum.get("balance"),itemId);
    }

    public Map<String,Object> dashboardMetrics(LocalDate today) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN DATE(issue_date)=? THEN total_value ELSE 0 END),0) today_cost,"
                        + "COALESCE(SUM(CASE WHEN YEARWEEK(issue_date)=YEARWEEK(?) THEN total_value ELSE 0 END),0) week_cost,"
                        + "COALESCE(SUM(CASE WHEN YEAR(issue_date)=YEAR(?) AND MONTH(issue_date)=MONTH(?) THEN total_value ELSE 0 END),0) month_cost,"
                        + "COALESCE(SUM(total_value),0) total_cost FROM dining_hall_consumption",
                (rs,row)->{Map<String,Object> m=new LinkedHashMap<>();m.put("today",nz(rs.getBigDecimal("today_cost")));m.put("week",nz(rs.getBigDecimal("week_cost")));
                    m.put("month",nz(rs.getBigDecimal("month_cost")));m.put("allTime",nz(rs.getBigDecimal("total_cost")));return m;},today,today,today,today);
    }

    public List<Map<String,Object>> dailyDashboard(LocalDate from,LocalDate to) {
        String sql="SELECT DATE(issue_date) day,COUNT(*) item_count,COALESCE(SUM(qty_issued),0) quantity,COALESCE(SUM(total_value),0) value "
                +"FROM dining_hall_consumption ";
        if(from!=null&&to!=null)sql+="WHERE DATE(issue_date) BETWEEN ? AND ? ";
        sql+="GROUP BY DATE(issue_date) ORDER BY day";
        org.springframework.jdbc.core.RowMapper<Map<String,Object>> mapper=
                (rs,row)->{Map<String,Object> m=new LinkedHashMap<>();m.put("date",rs.getDate("day"));m.put("count",rs.getInt("item_count"));
                    m.put("quantity",nz(rs.getBigDecimal("quantity")));m.put("value",nz(rs.getBigDecimal("value")));return m;};
        return from!=null&&to!=null?jdbc.query(sql,mapper,from,to):jdbc.query(sql,mapper);
    }

    public List<Map<String,Object>> sessionDashboard() {
        return jdbc.query("SELECT DATE(issue_date) day,session,COALESCE(SUM(qty_issued),0) quantity,COALESCE(SUM(total_value),0) value,COUNT(*) item_count "
                        + "FROM dining_hall_consumption GROUP BY DATE(issue_date),session ORDER BY day,FIELD(session,'Morning Drink','Break Fast','Lunch','Snacks','Staff Tea','Dinner','Special Event','Adjustment')",
                (rs,row)->{Map<String,Object> m=new LinkedHashMap<>();m.put("date",rs.getDate("day"));m.put("session",rs.getString("session"));m.put("quantity",nz(rs.getBigDecimal("quantity")));
                    m.put("value",nz(rs.getBigDecimal("value")));m.put("count",rs.getInt("item_count"));return m;});
    }

    public List<Map<String,Object>> consumptionReport(LocalDate from,LocalDate to,String mealSession) {
        StringBuilder sql=new StringBuilder("SELECT DATE(d.issue_date) day,d.session,i.Item_name,i.UOM,SUM(d.qty_issued) quantity,SUM(d.total_value) value "
                + "FROM dining_hall_consumption d JOIN item_master i ON i.Item_id=d.item_id WHERE DATE(d.issue_date) BETWEEN ? AND ? ");
        List<Object> args=new ArrayList<>(List.of(from,to));
        if(mealSession!=null&&!mealSession.isBlank()){sql.append("AND d.session=? ");args.add(mealSession);}
        sql.append("GROUP BY DATE(d.issue_date),d.session,d.item_id,i.Item_name,i.UOM ORDER BY day DESC,FIELD(d.session,'Morning Drink','Break Fast','Lunch','Snacks','Staff Tea','Dinner','Special Event','Adjustment'),i.Item_name");
        return jdbc.query(sql.toString(),(rs,row)->{Map<String,Object> m=new LinkedHashMap<>();m.put("date",rs.getDate("day"));m.put("session",rs.getString("session"));
            m.put("item",rs.getString("Item_name"));m.put("uom",rs.getString("UOM"));m.put("quantity",nz(rs.getBigDecimal("quantity")));m.put("value",nz(rs.getBigDecimal("value")));return m;},args.toArray());
    }

    public List<Map<String,Object>> consumptionOnDate(LocalDate date) {
        return jdbc.query("SELECT d.issue_id,d.issueno,d.item_id,d.po_item_id,d.department,d.issued_to,d.qty_issued,d.issue_date,d.remarks,d.unit_price,d.total_value,i.Item_name,i.UOM "
                        + "FROM dining_hall_consumption d LEFT JOIN item_master i ON i.Item_id=d.item_id WHERE DATE(d.issue_date)=? ORDER BY d.issue_id",
                (rs,row)->mapConsumption(rs),date);
    }

    public Map<String,Object> lockConsumption(int id,LocalDate date) {
        List<Map<String,Object>> rows=jdbc.query("SELECT issue_id,issueno,item_id,po_item_id,qty_issued,unit_price,department,issued_to,remarks "
                        + "FROM dining_hall_consumption WHERE issue_id=? AND DATE(issue_date)=? FOR UPDATE",(rs,row)->{
                    Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getInt("issue_id"));m.put("issueNo",rs.getString("issueno"));m.put("itemId",rs.getInt("item_id"));
                    m.put("poItemId",rs.getObject("po_item_id"));m.put("qty",nz(rs.getBigDecimal("qty_issued")));m.put("price",nz(rs.getBigDecimal("unit_price")));return m;
                },id,date);
        return rows.isEmpty()?null:rows.get(0);
    }

    public BigDecimal lockCurrentBalance(int itemId) {
        List<BigDecimal> rows=jdbc.query("SELECT balance_qty FROM stock WHERE item_id=? ORDER BY stock_id LIMIT 1 FOR UPDATE",(rs,row)->nz(rs.getBigDecimal(1)),itemId);
        if(rows.isEmpty())throw new IllegalArgumentException("Stock record is missing for item ID "+itemId+".");return rows.get(0);
    }

    public void updateConsumption(int id,String issueNo,int itemId,BigDecimal quantity,BigDecimal price,BigDecimal difference,String department,String recipient,String remarks) {
        Map<String,Object> stock=lockStock(itemId);
        if(stock==null)throw new IllegalArgumentException("Stock record is missing for item ID "+itemId+".");
        if(difference.signum()>0&&difference.compareTo((BigDecimal)stock.get("balance"))>0)
            throw new IllegalArgumentException("Updated quantity exceeds available stock for item ID "+itemId+".");
        BigDecimal total=quantity.multiply(price).setScale(2,java.math.RoundingMode.HALF_UP);
        jdbc.update("UPDATE dining_hall_consumption SET department=?,issued_to=?,qty_issued=?,remarks=?,total_value=? WHERE issue_id=?",
                department,recipient,quantity,remarks,total,id);
        jdbc.update("UPDATE stock_issues SET department=?,issued_to=?,qty_issued=?,remarks=?,total_value=? WHERE issueno=? AND item_id=?",
                department,recipient,quantity,remarks,total,issueNo,itemId);
        int updated=jdbc.update("UPDATE stock_ledger SET qty=?,remarks=? WHERE consumption_id=? AND item_id=? AND UPPER(TRIM(trans_type))='ISSUE'",
                quantity,remarks,id,itemId);
        if(updated==0)jdbc.update("INSERT INTO stock_ledger(consumption_id,item_id,trans_type,qty,remarks,trans_date,running_balance) "
                + "SELECT ?,?,'ISSUE',?,?,DATE(issue_date),0 FROM dining_hall_consumption WHERE issue_id=?",id,itemId,quantity,remarks,id);
        int stockUpdated=jdbc.update("UPDATE stock SET total_issued=COALESCE(total_issued,0)+?,balance_qty=COALESCE(balance_qty,0)-?,last_updated=NOW() WHERE stock_id=? AND balance_qty>=?",
                difference,difference,stock.get("id"),difference.max(BigDecimal.ZERO));
        if(stockUpdated!=1)throw new IllegalArgumentException("Stock changed during the update. Reload and try again.");
        recalculateLedger(itemId);
    }

    private void recalculateLedger(int itemId) {
        List<Map<String,Object>> entries=jdbc.query("SELECT ledger_id,trans_type,qty FROM stock_ledger WHERE item_id=? ORDER BY trans_date,ledger_id",
                (rs,row)->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getInt("ledger_id"));m.put("type",rs.getString("trans_type"));m.put("qty",nz(rs.getBigDecimal("qty")));return m;},itemId);
        BigDecimal balance=BigDecimal.ZERO;BigDecimal received=BigDecimal.ZERO;BigDecimal issued=BigDecimal.ZERO;
        for(Map<String,Object> e:entries){BigDecimal qty=(BigDecimal)e.get("qty");String type=String.valueOf(e.get("type"));
            if("RECEIPT".equalsIgnoreCase(type.trim())){received=received.add(qty);balance=balance.add(qty);}else if("ISSUE".equalsIgnoreCase(type.trim())){issued=issued.add(qty);balance=balance.subtract(qty);}
            jdbc.update("UPDATE stock_ledger SET running_balance=? WHERE ledger_id=?",balance,e.get("id"));}
        jdbc.update("UPDATE stock SET total_received=?,total_issued=?,balance_qty=?,last_updated=NOW() WHERE item_id=?",received,issued,balance,itemId);
    }

    private Map<String,Object> mapConsumption(ResultSet rs)throws SQLException{
        Map<String,Object> m=new LinkedHashMap<>();m.put("id",rs.getInt("issue_id"));m.put("issueNo",rs.getString("issueno"));m.put("itemId",rs.getInt("item_id"));m.put("poItemId",rs.getObject("po_item_id"));
        m.put("department",rs.getString("department"));m.put("recipient",rs.getString("issued_to"));m.put("quantity",nz(rs.getBigDecimal("qty_issued")));
        m.put("date",rs.getDate("issue_date").toLocalDate());m.put("remarks",rs.getString("remarks"));m.put("price",nz(rs.getBigDecimal("unit_price")));
        m.put("total",nz(rs.getBigDecimal("total_value")));m.put("item",rs.getString("Item_name"));m.put("uom",rs.getString("UOM"));return m;
    }
    private static BigDecimal nz(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
}