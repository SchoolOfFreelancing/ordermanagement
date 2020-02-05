package com.ordermanagement.ordermanagement.service.partOrder;

import java.util.List;

/**
 * Created by Sheyan Sandaruwan on 1/30/2020.
 */
public interface PartOrderManagementService {
    String orderParts() throws Exception;

    String retrievePendingResponse(String trackNum, int waitSecs, int intervalSecs) throws Exception;

    List getTransactionIds(String xml) throws Exception;

    List listOrders() throws Exception;
}
