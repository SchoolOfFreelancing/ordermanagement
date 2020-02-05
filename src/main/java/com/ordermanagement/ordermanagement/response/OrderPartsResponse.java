package com.ordermanagement.ordermanagement.response;

import java.util.List;

import com.ordermanagement.ordermanagement.model.Item;
import com.ordermanagement.ordermanagement.model.Source;

public class OrderPartsResponse {

	private List<Item> itemList;
	private List<Source> itemSources;
	
	public List<Item> getItemList() {
		return itemList;
	}
	public void setItemList(List<Item> itemList) {
		this.itemList = itemList;
	}
	public List<Source> getItemSources() {
		return itemSources;
	}
	public void setItemSources(List<Source> itemSources) {
		this.itemSources = itemSources;
	}
	
	
}
