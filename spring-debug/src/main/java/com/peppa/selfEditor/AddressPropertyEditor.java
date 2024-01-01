package com.peppa.selfEditor;


import java.beans.PropertyEditorSupport;

public class AddressPropertyEditor extends PropertyEditorSupport {
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] split = text.split("_");
		Address address=new Address();
		address.setProvince(split[0]);
		address.setCity(split[1]);
		address.setArea(split[2]);
		this.setValue(address);
	}
}
