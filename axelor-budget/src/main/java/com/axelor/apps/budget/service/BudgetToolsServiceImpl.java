/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.budget.service;

import com.axelor.apps.account.db.AccountConfig;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.repo.MoveRepository;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.budget.db.Budget;
import com.axelor.apps.budget.db.BudgetLine;
import com.axelor.apps.budget.db.repo.BudgetLevelRepository;
import com.axelor.apps.budget.exception.BudgetExceptionMessage;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.axelor.utils.date.DateTool;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import org.apache.commons.collections.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@RequestScoped
public class BudgetToolsServiceImpl implements BudgetToolsService {

  protected AccountConfigService accountConfigService;
  protected AppBudgetService appBudgetService;

  @Inject
  public BudgetToolsServiceImpl(AccountConfigService accountConfigService, AppBudgetService appBudgetService) {
    this.accountConfigService = accountConfigService;
    this.appBudgetService = appBudgetService;
  }

  @Override
  public boolean checkBudgetKeyAndRole(Company company, User user) throws AxelorException {
    if (company != null && user != null) {
      AccountConfig accountConfig = accountConfigService.getAccountConfig(company);
      if (!accountConfig.getEnableBudgetKey()
          || CollectionUtils.isEmpty(accountConfig.getBudgetDistributionRoleList())) {
        return true;
      }
      for (Role role : user.getRoles()) {
        if (accountConfig.getBudgetDistributionRoleList().contains(role)) {
          return true;
        }
      }
      for (Role role : user.getGroup().getRoles()) {
        if (accountConfig.getBudgetDistributionRoleList().contains(role)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean checkBudgetKeyAndRoleForMove(Move move) throws AxelorException {
    if (move != null) {
      return !(checkBudgetKeyAndRole(move.getCompany(), AuthUtils.getUser()))
          || move.getStatusSelect() == MoveRepository.STATUS_ACCOUNTED
          || move.getStatusSelect() == MoveRepository.STATUS_CANCELED;
    }
    return false;
  }

  @Override
  public String getBudgetExceedAlert(Budget budget, BigDecimal amount, LocalDate date) {

    String budgetExceedAlert = "";

    Integer budgetControlLevel = getBudgetControlLevel(budget);
    if (budget == null || budgetControlLevel == null) {
      return budgetExceedAlert;
    }
    BigDecimal budgetToCompare = BigDecimal.ZERO;
    String budgetName = budget.getName();

    switch (budgetControlLevel) {
      case BudgetLevelRepository.BUDGET_LEVEL_AVAILABLE_AMOUNT_BUDGET_LINE:
        for (BudgetLine budgetLine : budget.getBudgetLineList()) {
          if (DateTool.isBetween(budgetLine.getFromDate(), budgetLine.getToDate(), date)) {
            budgetToCompare = budgetLine.getAvailableAmount();
            budgetName +=
                    ' ' + budgetLine.getFromDate().toString() + ':' + budgetLine.getToDate().toString();
            break;
          }
        }
        break;
      case BudgetLevelRepository.BUDGET_LEVEL_AVAILABLE_AMOUNT_BUDGET:
        budgetToCompare = budget.getAvailableAmount();
        break;
      case BudgetLevelRepository.BUDGET_LEVEL_AVAILABLE_AMOUNT_BUDGET_SECTION:
        budgetToCompare = budget.getBudgetLevel().getTotalAmountAvailable();
        budgetName = budget.getBudgetLevel().getName();
        break;
      case BudgetLevelRepository.BUDGET_LEVEL_AVAILABLE_AMOUNT_BUDGET_GROUP:
        budgetToCompare = budget.getBudgetLevel().getParentBudgetLevel().getTotalAmountAvailable();
        budgetName = budget.getBudgetLevel().getParentBudgetLevel().getName();
        break;
      default:
        budgetToCompare =
                budget
                        .getBudgetLevel()
                        .getParentBudgetLevel()
                        .getParentBudgetLevel()
                        .getTotalAmountAvailable();
        budgetName =
                budget.getBudgetLevel().getParentBudgetLevel().getParentBudgetLevel().getName();
        break;
    }
    if (budgetToCompare.compareTo(amount) < 0) {
      budgetExceedAlert =
              String.format(
                      I18n.get(BudgetExceptionMessage.BUGDET_EXCEED_ERROR),
                      budgetName,
                      budgetToCompare,
                      budget
                              .getBudgetLevel()
                              .getParentBudgetLevel()
                              .getParentBudgetLevel()
                              .getCompany()
                              .getCurrency()
                              .getSymbol());
    }
    return budgetExceedAlert;
  }


  @Override
  public Integer getBudgetControlLevel(Budget budget) {

    if (appBudgetService.getAppBudget() == null
            || !appBudgetService.getAppBudget().getCheckAvailableBudget()) {
      return null;
    }

    if (budget != null
            && budget.getBudgetLevel() != null
            && budget.getBudgetLevel().getParentBudgetLevel() != null
            && budget.getBudgetLevel().getParentBudgetLevel().getGlobalBudget() != null
            && budget
            .getBudgetLevel()
            .getParentBudgetLevel()
            .getGlobalBudget()
            .getCheckAvailableSelect()
            != null
            && budget
            .getBudgetLevel()
            .getParentBudgetLevel()
            .getGlobalBudget()
            .getCheckAvailableSelect()
            != 0) {
      return budget
              .getBudgetLevel()
              .getParentBudgetLevel()
              .getGlobalBudget()
              .getCheckAvailableSelect();
    } else {
      return appBudgetService.getAppBudget().getCheckAvailableBudget()
              ? BudgetLevelRepository.BUDGET_LEVEL_AVAILABLE_AMOUNT_BUDGET_LINE
              : null;
    }
  }
}
