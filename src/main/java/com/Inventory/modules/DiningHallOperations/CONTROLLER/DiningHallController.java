package com.Inventory.modules.DiningHallOperations.CONTROLLER;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Inventory.modules.DiningHallOperations.SERVICE.DiningHallService;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.RoleAccess;
import com.Inventory.SESSION.SessionKeys;

@Controller
public class DiningHallController {
    private final DiningHallService service;
    private final PageAccessService access;
    public DiningHallController(DiningHallService service, PageAccessService access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping("/dining-hall/consumption/entry")
    public String entry(Model model, @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String department,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, department, company);
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("sessions", DiningHallService.SESSIONS.stream().filter(s -> !"Adjustment".equals(s) || RoleAccess.isGlobal(role)).toList());
        model.addAttribute("allowAdjustment", RoleAccess.isGlobal(role));
        try {
            model.addAttribute("issueNo", service.previewIssueNo());
            model.addAttribute("masterData", service.entryMasterData());
        } catch (DataAccessException ex) {
            model.addAttribute("issueNo", "");
            model.addAttribute("masterData", Map.of("categories", List.of(), "subcategories", List.of(), "items", List.of()));
            model.addAttribute("error", "Could not load Dining Hall item and stock data for the selected company.");
        }
        return "modules/DiningHallOperations/dining-entry";
    }

    @PostMapping("/dining-hall/consumption/entry")
    public String save(@RequestParam String issuedTo, @RequestParam String mealSession, @RequestParam String issueDate,
            @RequestParam(required=false) int[] itemId, @RequestParam(required=false) String[] quantity,
            @RequestParam(required=false) String[] remarks,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId, RedirectAttributes flash) {
        requireButton(userId, role, "/dining-hall/consumption/entry", "SAVE_DINING_CONSUMPTION");
        try {
            String number = service.create(issuedTo, mealSession, issueDate, itemId, quantity, remarks, role);
            flash.addFlashAttribute("success", "Dining Hall consumption " + number + " was saved. Stock and ledger were updated.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            flash.addFlashAttribute("error", "Consumption could not be saved. Confirm dining_hall_consumption, stock_issues, stock, and stock_ledger exist in this company database.");
        }
        return "redirect:/dining-hall/consumption/entry";
    }

    @GetMapping("/dining-hall/dashboard")
    public String dashboard(@RequestParam(required=false) String fromDate, @RequestParam(required=false) String toDate, Model model,
            @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String department,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, department, company);
        LocalDate today = LocalDate.now();
        model.addAttribute("fromDate", fromDate == null ? "" : fromDate);
        model.addAttribute("toDate", toDate == null ? "" : toDate);
        model.addAttribute("dailyRows", List.of());
        model.addAttribute("sessionRows", List.of());
        model.addAttribute("latestSessionDate", "");
        
        YearMonth dashboardMonth = YearMonth.from(today);
        model.addAttribute("dashboardMonth", dashboardMonth.toString());
        model.addAttribute("calendarWeeks", service.buildDiningCalendar(dashboardMonth, List.of()));
        model.addAttribute("calendarTotal", java.math.BigDecimal.ZERO);

        try {
            LocalDate from = blank(fromDate) ? null : parseOptional(fromDate, null);
            LocalDate to = blank(toDate) ? null : parseOptional(toDate, null);

            if (!blank(fromDate) || !blank(toDate)) {
                requireButton(userId, role, "/dining-hall/dashboard", "FILTER_DINING_DASHBOARD");
            }
            if ((from == null) != (to == null)) {
                throw new IllegalArgumentException("Select both From and To dates to filter the dashboard.");
            }
            if (from != null && from.isAfter(to)) {
                throw new IllegalArgumentException("From date must be on or before To date.");
            }

            model.addAttribute("metrics", service.dashboardMetrics(today));

            // Calendar Data
            List<Map<String, Object>> calendarRows = service.consumptionReport(dashboardMonth.toString(), null);
            model.addAttribute("calendarWeeks", service.buildDiningCalendar(dashboardMonth, calendarRows));
            model.addAttribute("calendarTotal", calendarRows.stream().map(row -> (java.math.BigDecimal) row.get("value"))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

            // Fetch DB rows
            List<Map<String, Object>> rawDaily = service.dailyDashboard(from, to);

            // Fill all dates in range (defaulting to full month or 7-30 day window if un-filtered)
            LocalDate startRange = (from != null) ? from : dashboardMonth.atDay(1);
            LocalDate endRange = (to != null) ? to : (today.isAfter(dashboardMonth.atEndOfMonth()) ? dashboardMonth.atEndOfMonth() : today);

            Map<LocalDate, java.math.BigDecimal> valueMap = new LinkedHashMap<>();
            for (Map<String, Object> r : rawDaily) {
                Object dObj = r.get("date");
                LocalDate d = (dObj instanceof java.sql.Date sqlDate) ? sqlDate.toLocalDate() : LocalDate.parse(dObj.toString());
                valueMap.put(d, (java.math.BigDecimal) r.get("value"));
            }

            List<Map<String, Object>> daily = new ArrayList<>();
            for (LocalDate date = startRange; !date.isAfter(endRange); date = date.plusDays(1)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date", java.sql.Date.valueOf(date));
                row.put("value", valueMap.getOrDefault(date, java.math.BigDecimal.ZERO));
                daily.add(row);
            }

            // Calculate Bar Percentages
            java.math.BigDecimal max = daily.stream()
                    .map(row -> (java.math.BigDecimal) row.get("value"))
                    .max(java.math.BigDecimal::compareTo)
                    .orElse(java.math.BigDecimal.ZERO);

            daily.forEach(row -> {
                java.math.BigDecimal value = (java.math.BigDecimal) row.get("value");
                int width = max.signum() == 0 ? 0 : value.multiply(new java.math.BigDecimal("100")).divide(max, 0, java.math.RoundingMode.HALF_UP).intValue();
                row.put("barPercent", width);
            });
            model.addAttribute("dailyRows", daily);

            // Session Rows Breakdown
            List<Map<String, Object>> allSessions = service.sessionDashboard();
            java.sql.Date latestDate = allSessions.isEmpty() ? null : (java.sql.Date) allSessions.get(allSessions.size() - 1).get("date");
            if (latestDate != null) {
                model.addAttribute("latestSessionDate", latestDate.toLocalDate());
                Map<String, Map<String, Object>> latestBySession = new LinkedHashMap<>();
                for (Map<String, Object> row : allSessions) {
                    if (!latestDate.equals(row.get("date"))) continue;
                    String source = String.valueOf(row.get("session"));
                    String label = "Morning Drink".equalsIgnoreCase(source) ? "Break Fast" : source;
                    Map<String, Object> target = latestBySession.computeIfAbsent(label, key -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("session", key);
                        m.put("quantity", java.math.BigDecimal.ZERO);
                        m.put("value", java.math.BigDecimal.ZERO);
                        m.put("count", 0);
                        return m;
                    });
                    target.put("quantity", ((java.math.BigDecimal) target.get("quantity")).add((java.math.BigDecimal) row.get("quantity")));
                    target.put("value", ((java.math.BigDecimal) target.get("value")).add((java.math.BigDecimal) row.get("value")));
                    target.put("count", (Integer) target.get("count") + (Integer) row.get("count"));
                }
                model.addAttribute("sessionRows", new ArrayList<>(latestBySession.values()));
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("metrics", Map.of("today", 0, "week", 0, "month", 0, "allTime", 0));
        } catch (DataAccessException ex) {
            model.addAttribute("error", "Could not load dining consumption dashboard for the selected company.");
            model.addAttribute("metrics", Map.of("today", 0, "week", 0, "month", 0, "allTime", 0));
        }
        return "modules/DiningHallOperations/dining-dashboard";
    }

    @GetMapping("/dining-hall/consumption/report")
    public String report(@RequestParam(required=false) String reportMonth, @RequestParam(required=false) String mealSession, Model model,
            @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String department,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, department, company);
        String month = blank(reportMonth) ? YearMonth.now().toString() : reportMonth;
        model.addAttribute("reportMonth", month);
        model.addAttribute("mealSession", mealSession == null ? "" : mealSession);
        model.addAttribute("sessions", DiningHallService.SESSIONS);
        model.addAttribute("reportRows", List.of());
        model.addAttribute("calendarWeeks", List.of());
        model.addAttribute("reportTotal", java.math.BigDecimal.ZERO);
        try {
            YearMonth selectedMonth = YearMonth.parse(month);
            List<Map<String, Object>> rows = List.of();
            if (!blank(reportMonth)) {
                requireButton(userId, role, "/dining-hall/consumption/report", "FILTER_DINING_REPORT");
                rows = service.consumptionReport(month, mealSession);
            }
            java.math.BigDecimal total = rows.stream().map(row -> (java.math.BigDecimal) row.get("value")).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            model.addAttribute("reportRows", rows);
            model.addAttribute("reportTotal", total);
            model.addAttribute("calendarWeeks", service.buildDiningCalendar(selectedMonth, rows));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("calendarWeeks", service.buildDiningCalendar(YearMonth.now(), List.of()));
        } catch (DataAccessException ex) {
            model.addAttribute("error", "Could not load the Dining Hall report for the selected company.");
            model.addAttribute("calendarWeeks", service.buildDiningCalendar(YearMonth.now(), List.of()));
        }
        return "modules/DiningHallOperations/dining-report";
    }

    @GetMapping("/dining-hall/consumption/edit")
    public String edit(@RequestParam(required=false) String selectedDate, @RequestParam(required=false) String msg, Model model,
            @RequestAttribute(name=SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role,
            @RequestAttribute(name=SessionKeys.REQUEST_DEPARTMENT, required=false) String department,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_COMPANY) String company) {
        session(model, username, role, department, company);
        model.addAttribute("selectedDate", selectedDate == null ? "" : selectedDate);
        model.addAttribute("consumptions", List.of());
        if ("updated".equals(msg)) model.addAttribute("success", "Selected consumption entries were updated.");
        if ("notfound".equals(msg)) model.addAttribute("error", "No consumption records were found for that date.");
        try {
            if (!blank(selectedDate)) {
                requireButton(userId, role, "/dining-hall/consumption/edit", "LOAD_DINING_EDIT");
                model.addAttribute("consumptions", service.consumptionOnDate(selectedDate));
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            model.addAttribute("error", "Could not load consumption rows. Check dining_hall_consumption and item_master.");
        }
        return "modules/DiningHallOperations/dining-edit";
    }

    @PostMapping("/dining-hall/consumption/edit")
    public String update(@RequestParam String selectedDate, @RequestParam(required=false) List<Integer> selectedIssueId,
            @RequestParam MultiValueMap<String, String> params,
            @RequestAttribute(name=SessionKeys.REQUEST_USER_ID, required=false) Integer userId,
            @RequestAttribute(name=SessionKeys.REQUEST_ROLE, required=false) String role, RedirectAttributes flash) {
        requireButton(userId, role, "/dining-hall/consumption/edit", "SAVE_DINING_EDIT");
        try {
            int count = service.update(LocalDate.parse(selectedDate), selectedIssueId, params);
            flash.addFlashAttribute("success", count + " consumption entr" + (count == 1 ? "y was" : "ies were") + " updated.");
        } catch (DateTimeParseException ex) {
            flash.addFlashAttribute("error", "Select a valid consumption date.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            flash.addFlashAttribute("error", "Update failed. Consumption, issue, stock, and ledger changes were rolled back.");
        }
        flash.addAttribute("selectedDate", selectedDate);
        return "redirect:/dining-hall/consumption/edit";
    }

    private void requireButton(Integer id, String role, String path, String code) {
        if (!access.hasButtonAccess(id, role, path, code)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission for this Dining Hall action.");
        }
    }

    private void session(Model model, String user, String role, String department, String company) {
        model.addAttribute("loggedInUser", user);
        model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department);
        model.addAttribute("selectedCompany", company);
    }

    private LocalDate parseOptional(String value, LocalDate fallback) {
        if (blank(value)) return fallback;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Enter a valid date.");
        }
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }
}