package com.hunyuan.sa.bpm.businesscontract;

import com.hunyuan.sa.bpm.module.businesscontract.domain.visual.*;
import java.util.List;

final class BusinessObjectFixtures {
    private BusinessObjectFixtures() {}
    static BpmBusinessObjectDraft expense() {
        FieldPresentation amountView = new FieldPresentation("NUMBER", "请输入申请金额", "元", List.of());
        BusinessObjectField amount = new BusinessObjectField("amount", "申请金额", "DECIMAL", true, "INTERNAL", false, amountView);
        BusinessObjectField reason = new BusinessObjectField("reason", "申请事由", "STRING", true, "INTERNAL", false, new FieldPresentation("TEXTAREA", "请输入申请事由", null, List.of()));
        BusinessObjectField approvalNote = new BusinessObjectField("approvalNote", "审批意见", "STRING", false, "INTERNAL", false, new FieldPresentation("TEXTAREA", null, null, List.of()));
        BusinessObjectField itemName = new BusinessObjectField("itemName", "费用项目", "STRING", true, "INTERNAL", false, new FieldPresentation("INPUT", null, null, List.of()));
        return new BpmBusinessObjectDraft("expense", "费用申请", "员工费用报销", "HUNYUAN", "EXPENSE", 0L,
                new BusinessKeyRule("FY", "yyyyMMdd", 4), List.of(amount, reason), List.of(),
                List.of(approvalNote), new LineItemSchema("费用明细", 1, 20, List.of(itemName)),
                new AttachmentRule(5, 20, List.of("pdf", "jpg", "png"), false),
                new DataChangeRule("FIELD_CONTROLLED", List.of("approvalNote")));
    }
}
