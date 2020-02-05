package com.ordermanagement.ordermanagement.service.partOrder;

import java.util.List;

import com.ordermanagement.ordermanagement.request.OrderPartsRequest;

/**
 * Created by Sheyan Sandaruwan on 1/30/2020.
 */
public interface TestPartOrderManagementService {
    String orderParts(OrderPartsRequest order) throws Exception;

    String retrievePendingResponse(String trackNum, int waitSecs, int intervalSecs) throws Exception;

    List getTransactionIds(String xml) throws Exception;

    List listOrders() throws Exception;
}
