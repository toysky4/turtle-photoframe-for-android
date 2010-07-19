package net.greenturtles.photoframe;

public class RegularSizeWidget_4x4 extends RegularSizeWidgetBase {
	@Override
	public String getUriSchemaId() {
		return "images_widget_4x4";
	}

	@Override
	public String getOnClickAction() {
		return "net.greenturtles.photoframe.RegularSizeWidget_4x4.onclick";
	}
}
