package net.greenturtles.photoframe;

public class RegularSizeWidget_2x2 extends RegularSizeWidgetBase {
	@Override
	public String getUriSchemaId() {
		return "images_widget_2x2";
	}

	@Override
	public String getOnClickAction() {
		return "net.greenturtles.photoframe.RegularSizeWidget_2x2.onclick";
	}
}
