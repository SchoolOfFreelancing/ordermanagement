package com.ordermanagement.ordermanagement.model;

public class Item {
	
	private String partNumber;
	private String lineCode;
	private int qtyNeeded;
	
	public int getQtyNeeded() {
		return qtyNeeded;
	}
	public void setQtyNeeded(int qtyNeeded) {
		this.qtyNeeded = qtyNeeded;
	}
	public String getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}
	public String getLineCode() {
		return lineCode;
	}
	public void setLineCode(String lineCode) {
		this.lineCode = lineCode;
	}
	
}
