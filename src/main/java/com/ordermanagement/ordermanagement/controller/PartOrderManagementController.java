package com.ordermanagement.ordermanagement.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ordermanagement.ordermanagement.request.OrderPartsRequest;
import com.ordermanagement.ordermanagement.response.OrderPartsResponse;
import com.ordermanagement.ordermanagement.service.partOrder.PartOrderManagementService;
import com.ordermanagement.ordermanagement.service.partOrder.TestPartOrderManagementService;

/**
 * Created by Sheyan Sandaruwan on 1/30/2020.
 */
@RestController
@RequestMapping("/part-order-management")
public class PartOrderManagementController {

    @Autowired
    private PartOrderManagementService partOrderManagementService;
    

    @Autowired
    private TestPartOrderManagementService testPartOrderManagementService;
    
    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    @RequestMapping("/order-parts")
    public Object orderParts(@Valid @RequestBody OrderPartsRequest request) throws Exception {
    	
    	String response = testPartOrderManagementService.orderParts(request);
        return response;
    }
    
    @GetMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    @RequestMapping("/retrieve-pending-response/{trackNum}/{waitSecs}/{intervalSecs}")
    public String retrievePendingResponse(@PathVariable("trackNum") String trackNum, @PathVariable("waitSecs") int waitSecs, @PathVariable("intervalSecs") int intervalSecs) throws Exception {
        return partOrderManagementService.retrievePendingResponse(trackNum, waitSecs, intervalSecs);
    }
    
    @GetMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    @RequestMapping("/get-transaction-ids/{xml}")
    public List getTransactionIds(@PathVariable("xml") String xml) throws Exception {
        return partOrderManagementService.getTransactionIds(xml);
    }
    
    @GetMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    @RequestMapping("/list-orders")
    public List listOrders() throws Exception {
        return partOrderManagementService.listOrders();
    }
    
    
}
