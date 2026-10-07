package com.Inventory.modules.INDENT.CONTROLLER;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.Inventory.Bean.IndentBean;
import com.Inventory.modules.INDENT.SERVICE.IndentService;
import com.Inventory.SERVICE.PageAccessService;
import com.Inventory.SESSION.SessionKeys;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/IndentServlet")
public class IndentServlet {

    @Autowired
    private IndentService indentService;
    @Autowired
    private PageAccessService pageAccessService;

    /* =========================================================
       LOAD INDENT PAGE
       ========================================================= */
    @GetMapping
    public String doGet(
            Model model,
            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role,
            @RequestAttribute(name = SessionKeys.REQUEST_DEPARTMENT, required = false) String department,
            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name = SessionKeys.REQUEST_COMPANY) String company) {

        model.addAttribute(
                "nextIndentNo",
                indentService.getNextIndentNumber());

        model.addAttribute(
                "masterData",
                indentService.getMasterData(
                        role,
                        department));

        model.addAttribute("loggedInUser", username);
        model.addAttribute("selectedCompany", company);
        model.addAttribute("userRole", role == null ? "" : role);
        model.addAttribute("userDepartment", department == null ? "" : department);

        return "modules/INDENT/indent";
    }

    /* =========================================================
       LOAD CATEGORIES FOR SELECTED DEPARTMENT
       ========================================================= */
    @GetMapping("/categories")
    @ResponseBody
    public List<IndentBean> getCategories(
            @RequestParam String department) {

        return indentService.getCategoriesByDepartment(department);
    }

    /* =========================================================
       LOAD SUB-CATEGORIES
       ========================================================= */
    @GetMapping("/subcategories")
    @ResponseBody
    public List<IndentBean> getSubCategories(
            @RequestParam String department,
            @RequestParam String category) {

        return indentService.getSubCategoriesByDepartmentAndCategory(
                department,
                category);
    }

    /* =========================================================
       LOAD ITEMS
       ========================================================= */
    @GetMapping("/items")
    @ResponseBody
    public List<IndentBean> getItems(
            @RequestParam String category,
            @RequestParam String subCategory) {

        return indentService.getItemsByCategoryAndSubCategory(
                category,
                subCategory);
    }

    /* =========================================================
       SAVE INDENT
       ========================================================= */
    @PostMapping
    public String doPost(
            @RequestParam String date,
            @RequestParam String indentType,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String itemIds,
            @RequestParam(required = false) String itemNames,
            @RequestParam(required = false) String quantities,
            @RequestParam(required = false) String purposes,
            @RequestParam(required = false) String uoms,
            @RequestAttribute(name = SessionKeys.REQUEST_USERNAME) String username,
            @RequestAttribute(name = SessionKeys.REQUEST_USER_ID, required = false) Integer userId,
            @RequestAttribute(name = SessionKeys.REQUEST_ROLE, required = false) String role) {

        if (!pageAccessService.hasButtonAccess(userId, role, "/IndentServlet", "SAVE_INDENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to save indents.");
        }

        try {

            String indentNo = indentService.saveIndent(
                    date,
                    department,
                    username,
                    indentType,
                    itemIds,
                    itemNames,
                    quantities,
                    purposes,
                    uoms);

            System.out.println(
                    "INDENT SAVED SUCCESSFULLY: "
                    + indentNo);

            return "redirect:/Home";

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to save indent: "
                    + e.getMessage(),
                    e);
        }
    }
}
