package com.axelor.apps.budget.service.invoice;

import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.AccountingSituationService;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.fixedasset.FixedAssetGenerationService;
import com.axelor.apps.account.service.invoice.InvoiceTermService;
import com.axelor.apps.account.service.invoice.workflow.ventilate.VentilateAdvancePaymentState;
import com.axelor.apps.account.service.invoice.workflow.ventilate.WorkflowVentilationService;
import com.axelor.apps.account.service.move.MoveCreateFromInvoiceService;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.budget.service.AppBudgetService;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class VentilateBudgetState extends VentilateAdvancePaymentState {

    protected AppBudgetService appBudgetService;
    protected BudgetInvoiceService budgetInvoiceService;
    @Inject
    public VentilateBudgetState(SequenceService sequenceService, MoveCreateFromInvoiceService moveCreateFromInvoiceService, AccountConfigService accountConfigService, AppAccountService appAccountService, InvoiceRepository invoiceRepo, WorkflowVentilationService workflowService, UserService userService, FixedAssetGenerationService fixedAssetGenerationService, InvoiceTermService invoiceTermService, AccountingSituationService accountingSituationService,
                                AppBudgetService appBudgetService, BudgetInvoiceService budgetInvoiceService) {
        super(sequenceService, moveCreateFromInvoiceService, accountConfigService, appAccountService, invoiceRepo, workflowService, userService, fixedAssetGenerationService, invoiceTermService, accountingSituationService);
        this.appBudgetService = appBudgetService;
        this.budgetInvoiceService = budgetInvoiceService;
    }

    @Override
    public void process() throws AxelorException {
        if (appBudgetService.getAppBudget() != null) {
            budgetInvoiceService.autoComputeBudgetDistribution(invoice);
        }
        super.process();
    }
}
