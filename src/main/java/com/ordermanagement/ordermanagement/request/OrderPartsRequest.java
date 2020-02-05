package com.ordermanagement.ordermanagement.request;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.ordermanagement.ordermanagement.model.Item;
import com.ordermanagement.ordermanagement.model.Supplier;

public class OrderPartsRequest {

	private Item[] itemList;
	private Supplier[] supplierList;

	public Item[] getItemList() {
		return itemList;
	}
	public void setItemList(Item[] itemList) {
		this.itemList = itemList;
	}
	public Supplier[] getSupplierList() {
		return supplierList;
	}
	public void setSupplierList(Supplier[] supplierList) {
		this.supplierList = supplierList;
	}
	
	@Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
	
	
	
}
