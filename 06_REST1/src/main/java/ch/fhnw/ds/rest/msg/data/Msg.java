package ch.fhnw.ds.rest.msg.data;

import java.time.LocalDate;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import io.github.threetenjaxb.core.LocalDateXmlAdapter;

@XmlRootElement // neccessary for application/xml mapping
public class Msg {
	private String text;
	private LocalDate date;

	public Msg() {
	}

	public Msg(String text) {
		this(text, LocalDate.now());
	}

	public Msg(String text, LocalDate date) {
		this.text = text;
		this.date = date;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@XmlJavaTypeAdapter(LocalDateXmlAdapter.class) // necessary for XML and new dates only
	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

}
