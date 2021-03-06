package ch.fhnw.ds.rest.msg.resources;

/*

Resource /msg
=============
curl -X GET -i http://localhost:9998/msg
curl -X GET -i -H "Accept: text/plain"       http://localhost:9998/msg
curl -X GET -i -H "Accept: application/json" http://localhost:9998/msg
curl -X GET -i -H "Accept: application/xml"  http://localhost:9998/msg
curl -X GET -i -H "Accept: application/xstream" http://localhost:9998/msg

curl -X DELETE   http://localhost:9998/msg

curl -X PUT -i -H "Content-Type: application/json" --data "{\"text\":\"JSON message\"}" http://localhost:9998/msg
curl -X PUT -i -H "Content-Type: application/xml"  --data "<msg><text>XML message</text></msg>" http://localhost:9998/msg
curl -X PUT -i -H "Content-Type: application/xstream"  --data "<ch.fhnw.ds.rest.msg.data.Msg><text>XStream message</text></ch.fhnw.ds.rest.msg.data.Msg>" http://localhost:9998/msg

curl -X PUT -i -H "Content-Type: text/plain" --data "msg=Plain Text message" http://localhost:9998/msg
curl -X PUT -i     --data msg=Hallo http://localhost:9998/msg
 
curl -X POST -i -H "Content-Type: application/json" --data "{\"text\":\"json\"}" http://localhost:9998/msg

Resource /msg/{id}
==================
curl -X GET -i http://localhost:9998/msg/Dominik
curl -X GET -i http://localhost:9998/msg/Dominik/headers


curl -X OPTIONS -i http://localhost:9998/msg
curl --head -i http://localhost:9998/msg

*/

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.net.URI;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import com.thoughtworks.xstream.XStream;

import ch.fhnw.ds.rest.msg.data.Msg;

@Singleton
@Path("/msg")
public class MsgResource {
	private XStream xstream = new XStream();
	
	private String msg = "Hello, world!";

	public MsgResource() {
		System.out.println("MsgResource() called");
	}

	// GET on /msg
	// ===========

	@GET
	@Produces("text/plain")
	public String getPlain() {
		return msg + "\n";
	}

	@GET
	@Produces("text/html")
	public String getHtml() {
		StringBuffer buf = new StringBuffer();
		buf.append("<html><body><h1>Message Text</h1>" + msg + "<br>");
		buf.append("<form method=\"POST\" action=\"/msg\">");
		buf.append("<p>Text: <input name=\"msg\" type=\"text\" size=20/>");
		buf.append("<input type=\"submit\" value=\"Submit\" />");
		buf.append("</form>");
		buf.append("</body></html>");
		return buf.toString();
	}

	@GET
	@Produces("application/xstream")
	public String getXml() {
		return xstream.toXML(new Msg(msg));
	}

	@GET
	@Produces({"application/json", "application/xml"})
	public Msg getJson() {
		return new Msg(msg);
	}

	// PUT on /msg
	// ===========

	@PUT
	@Consumes("text/plain")
	public void setTextPlain(String msg) {
		this.msg = msg;
	}

	@PUT
	@Consumes( { "application/json", "application/xml" } )
	public void setTextJson(Msg message) {
		msg = message.getText();
	}

	@PUT
	@Consumes( "application/xstream" )
	public void setTextXml(String text) {
		Msg message = (Msg)xstream.fromXML(text);
		msg = message.getText();
	}

	@PUT
	@Consumes("application/x-www-form-urlencoded")
	@Produces("text/xml")
	public String setTextForm(@FormParam("msg") String msg) {
		this.msg = msg;

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		XMLEncoder enc = new XMLEncoder(stream);
		enc.writeObject(msg);
		enc.close();
		return new String(stream.toByteArray()) + "\n";
	}

	// POST on /msg (used for forms)
	// =============================

	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces("text/html")
	public String doPost(@FormParam("msg") String msg) {
		this.msg = msg;
		return getHtml();
	}

	@POST
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json")
	public Msg doPost2(@FormParam("msg") String msg) {
		this.msg = msg;
		return new Msg(this.msg);
	}

	@POST
	@Consumes( { "application/xml", "application/json" })
	public Response createNewMessage(@Context UriInfo uriInfo, Msg message) {
		URI location = uriInfo.getAbsolutePathBuilder().path(message.getText()).build();
		return Response.created(location).build();
	}

	// DELETE on /msg
	// ==============

	@DELETE
	@Produces("text/plain")
	public String onDelete() {
		msg = null;
		return "Message deleted.\n";
	}

	// GET on /msg/cc
	// ==============

	@GET
	@Path("cc")
	@Produces("text/plain")
	public Response getPlain2() {
		ResponseBuilder builder = Response.ok(msg + "\n");
		CacheControl cc = new CacheControl();
		cc.setMaxAge(1000); // HTTP max-age field, in seconds
		cc.setNoTransform(true);
		cc.setPrivate(true);
		builder.cacheControl(cc);
		return builder.build();
	}

	// GET on /msg/{id} and on /msg/{id}/headers
	// =========================================

	@GET
	@Produces("text/plain")
	@Path("{id}")
	public String readDetailsInfo(@PathParam("id") String path, @Context Request r) {
		return msg + ": " + path + "\n";
	}

	@GET
	@Produces("text/plain")
	@Path("{id}/headers")
	public String readDetailHeaders(@PathParam("id") String path,
			@Context HttpHeaders headers) {
		StringBuffer buf = new StringBuffer();
		buf.append("Headers of request " + path + "\n\n");
		MultivaluedMap<String, String> map = headers.getRequestHeaders();
		for (String key : map.keySet()) {
			buf.append(key + ": " + map.getFirst(key) + "\n");
		}
		return buf.toString();
	}

}
