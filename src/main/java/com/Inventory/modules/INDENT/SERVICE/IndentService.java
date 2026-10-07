package com.Inventory.modules.INDENT.SERVICE;

import java.util.*;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inventory.Bean.IndentBean;
import com.Inventory.modules.INDENT.DAO.IndentDAO;

@Service
public class IndentService {

    private static final Logger log = LoggerFactory.getLogger(IndentService.class);
    private final IndentDAO dao;

    public IndentService(IndentDAO dao) {
        this.dao = dao;
    }

    public int getNextIndentNumber() {
        return dao.getNextIndentNumber();
    }

    public IndentBean getMasterData(String role, String department) {
        return dao.getMasterData(role, department);
    }

    public List<IndentBean> getCategoriesByDepartment(String department) {
        validateRequired(department, "Department is required.");
        return dao.getCategoriesByDepartment(department.trim());
    }

    public List<IndentBean> getSubCategoriesByDepartmentAndCategory(String department, String category) {
        validateRequired(department, "Department is required.");
        validateRequired(category, "Category is required.");
        return dao.getSubCategoriesByDepartmentAndCategory(department.trim(), category.trim());
    }

    public List<IndentBean> getItemsByCategoryAndSubCategory(String category, String subCategory) {
        validateRequired(category, "Category is required.");
        validateRequired(subCategory, "Sub-category is required.");
        return dao.getItemsByCategoryAndSubCategory(category.trim(), subCategory.trim());
    }

    @Transactional
    public String saveIndent(String date, String department, String requestedBy, 
                             String indentType, String itemIds, String itemNames, 
                             String quantities, String purposes, String uoms) {

        validateRequired(department, "Department is required.");
        validateRequired(date, "Date is required.");
        validateRequired(indentType, "Indent type is required.");

        String actualRequestedBy = isBlank(requestedBy) ? "SYSTEM" : requestedBy.trim();
        List<IndentBean> items = buildItemList(itemIds, itemNames, quantities, purposes, uoms);

        if (items.isEmpty()) {
            throw new IllegalArgumentException("No valid items were submitted.");
        }

        String indentNo = dao.saveIndent(date.trim(), department.trim(), actualRequestedBy, indentType.trim(), items);
        log.info("INDENT SAVED SUCCESSFULLY: {} | items={}", indentNo, items.size());
        
        return indentNo;
    }

    private List<IndentBean> buildItemList(String ids, String names, String qtys, String purposes, String uoms) {
        String[] idArr = split(ids);
        String[] nameArr = split(names);
        String[] qtyArr = split(qtys);
        String[] purposeArr = split(purposes);
        String[] uomArr = split(uoms);

        int max = Math.min(Math.min(idArr.length, nameArr.length), qtyArr.length);

        return IntStream.range(0, max)
                .mapToObj(i -> parseItemRow(i, idArr[i], nameArr[i], qtyArr[i], 
                                           getValue(purposeArr, i), getValue(uomArr, i)))
                .filter(Objects::nonNull)
                .toList();
    }

    private IndentBean parseItemRow(int index, String idStr, String nameStr, String qtyStr, String purpose, String uom) {
        if (isBlank(idStr) || isBlank(nameStr) || isBlank(qtyStr)) return null;

        try {
            double quantity = Double.parseDouble(qtyStr.trim());
            if (quantity <= 0) return null;

            IndentBean bean = new IndentBean();
            bean.setItemId(Integer.parseInt(idStr.trim()));
            bean.setItemName(nameStr.trim());
            bean.setQuantity(quantity);
            bean.setPurpose(purpose);
            bean.setUom(uom);
            return bean;
        } catch (NumberFormatException e) {
            log.warn("Invalid numeric value in indent row: {}", index);
        } catch (Exception e) {
            log.warn("Invalid indent item row {}: {}", index, e.getMessage());
        }
        return null;
    }

    private String[] split(String value) {
        return isBlank(value) ? new String[0] : value.split(",", -1);
    }

    private String getValue(String[] array, int index) {
        return (index < array.length && array[index] != null) ? array[index].trim() : "";
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void validateRequired(String value, String message) {
        if (isBlank(value)) throw new IllegalArgumentException(message);
    }
}