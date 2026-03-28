package com.example.tobe;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchCountListener implements QueryExecutionListener {

    private final AtomicInteger batchExecutionCount = new AtomicInteger(0);
    private final AtomicInteger batchedStatementCount = new AtomicInteger(0);
    private final AtomicInteger selectCount = new AtomicInteger(0);

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {}

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (execInfo.isBatch()) {
            batchExecutionCount.incrementAndGet();                        // executeBatch() 호출 횟수
            batchedStatementCount.addAndGet(execInfo.getBatchSize());    // 묶인 statement 수
        }
        // SELECT 카운팅 추가
        for (QueryInfo queryInfo : queryInfoList) {
            String query = queryInfo.getQuery().trim().toUpperCase();
            if (query.startsWith("SELECT")) {
                selectCount.incrementAndGet();
            }
        }
    }

    public int getBatchExecutionCount() { return batchExecutionCount.get(); }
    public int getBatchedStatementCount() { return batchedStatementCount.get(); }
    public int getSelectCount() { return selectCount.get(); }
    public void reset() {
        batchExecutionCount.set(0);
        batchedStatementCount.set(0);
        selectCount.set(0);
    }
}
