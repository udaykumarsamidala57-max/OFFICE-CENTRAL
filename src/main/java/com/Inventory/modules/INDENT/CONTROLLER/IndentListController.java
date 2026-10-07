package com.Inventory.modules.INDENT.CONTROLLER;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.Inventory.modules.INDENT.DAO.IndentListDAO;
import com.Inventory.SESSION.SessionKeys;

@Controller
public class IndentListController {
    private final IndentListDAO indentListDAO;

    public IndentListController(IndentListDAO indentListDAO) {
        this.indentListDAO = indentListDAO;
    }

    @GetMapping("/IndentlistServlet")
    public String showIndentList(
            Model model,
            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
            @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        model.addAttribute("indents", indentListDAO.findRecent(role, department));
        model.addAttribute("loggedInUser", username);
        model.addAttribute("selectedCompany", company);
        model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department);
        return "modules/INDENT/indent-list";
    }

    @GetMapping("/IndentPrint")
    public String printIndent(
            @RequestParam("IndentNumber") String indentNumber,
            Model model,
            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
            @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {
        var items = indentListDAO.findByIndentNo(indentNumber, role, department);
        model.addAttribute("items", items);
        model.addAttribute("indentNumber", indentNumber);
        model.addAttribute("loggedInUser", username);
        model.addAttribute("selectedCompany", company);
        return "modules/INDENT/indent-print";
    }
}
