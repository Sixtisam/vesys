package ch.fhnw.ds.internet.currency;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

//uses the currency converter provided at https://www.calculator.net/currency-calculator.html
public class CurrencyConverterNet { // extends javafx.application.Application {
	
	public static void main(String args[]) {
//		launch(args);
		new JFXPanel(); // this will prepare JavaFX toolkit and environment
		CurrencyConverterNet gui = new CurrencyConverterNet();
		Platform.runLater(() -> {
			gui.start(new Stage());
		});
	}

//	@Override
	public void start(Stage stage) {
		stage.setTitle("Currency Converter");      
		GridPane root = new GridPane();
		root.setHgap(10);
		root.setVgap(10);
		root.setPadding(new Insets(10));
		
		ObservableList<String> data = FXCollections.observableArrayList();
		currencies.forEach((key, entry) -> data.add(key + " - " + entry));

		ComboBox<String> from = new ComboBox<>(data);
		ComboBox<String> to = new ComboBox<>(data);
		
		TextField amount = new TextField();
		TextField result = new TextField();
		result.setEditable(false);
		
		ObjectProperty<Double> amountProp = new SimpleObjectProperty<>(null);
		amount.textProperty().bindBidirectional(amountProp, new StringDoubleConverter());
		amount.setTextFormatter(new TextFormatter<Double>(new StringDoubleConverter(), 0d, new DoubleFilter()));

		Button submit = new Button("Submit");
		submit.setMaxWidth(Double.MAX_VALUE);
		submit.disableProperty().bind(amountProp.isNull().or(amountProp.isEqualTo(0.0)));
		submit.setOnAction(e -> {
			result.setText("computing...");
			new Thread(() -> {
				result.setText(computeResult(
					amount.getText(),
					from.getSelectionModel().getSelectedItem().substring(0, 3),
					to.getSelectionModel().getSelectedItem().substring(0, 3)
				));
			}).start();
		});
		
		from.getSelectionModel().select("CHF - " + currencies.get("CHF"));
		to.getSelectionModel().select("EUR - " + currencies.get("EUR"));
		
		root.add(from, 0, 0);
		root.add(to, 1, 0);
		
		root.add(amount, 0,1);
		root.add(result, 1,1);

		root.add(submit, 0,2,2,1);

		Scene scene = new Scene(root, 520, 120);
		stage.setScene(scene);
		stage.show();
	}

	static String computeResult(String amount, String from, String to){
		String TOKEN = "green";
		try {
			String query = "https://www.calculator.net/currency-calculator.html?eamount="+amount+"&efrom="+from+"&eto="+to+"&x=5";
			System.out.println(query);
			
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(new URI(query)).GET().build();
			HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
			return response.body()
					.filter(line -> line.contains(TOKEN))
					.findFirst()
					.map(line -> {
						int pos = line.indexOf(TOKEN, 0);
						pos = line.indexOf("<b>", pos);
						String res = line.substring(pos+3);
						return res.substring(0, res.indexOf("<"));
			}).orElse("no result found");
		} catch (Exception e) {
			String msg = e.getMessage();
			return "".equals(msg) ? e.toString() : msg;
		}
	}

	static Map<String, String> currencies = new TreeMap<>();
	static {
		currencies.put("AED", "United Arab Emirates Dirham");
		currencies.put("AFN", "Afghan Afghani");
		currencies.put("ALL", "Albanian Lek");
		currencies.put("AMD", "Armenian Dram");
		currencies.put("ANG", "Netherlands Antillean Guilder");
		currencies.put("AOA", "Angolan Kwanza");
		currencies.put("ARS", "Argentine Peso");
		currencies.put("AUD", "Australian Dollar");
		currencies.put("AWG", "Aruban Florin");
		currencies.put("AZN", "Azerbaijani Manat");
		currencies.put("BAM", "Bosnia-Herzegovina Convertible Mark");
		currencies.put("BBD", "Barbadian Dollar");
		currencies.put("BDT", "Bangladeshi Taka");
		currencies.put("BGN", "Bulgarian Lev");
		currencies.put("BHD", "Bahraini Dinar");
		currencies.put("BIF", "Burundian Franc");
		currencies.put("BMD", "Bermudan Dollar");
		currencies.put("BND", "Brunei Dollar");
		currencies.put("BOB", "Bolivian Boliviano");
		currencies.put("BRL", "Brazilian Real");
		currencies.put("BSD", "Bahamian Dollar");
		currencies.put("BTC", "Bitcoin");
		currencies.put("BTN", "Bhutanese Ngultrum");
		currencies.put("BWP", "Botswanan Pula");
		currencies.put("BYN", "Belarusian Ruble");
		currencies.put("BZD", "Belize Dollar");
		currencies.put("CAD", "Canadian Dollar");
		currencies.put("CDF", "Congolese Franc");
		currencies.put("CHF", "Swiss Franc");
		currencies.put("CLF", "Chilean Unit of Account (UF)");
		currencies.put("CLP", "Chilean Peso");
		currencies.put("CNH", "Chinese Yuan (Offshore)");
		currencies.put("CNY", "Chinese Yuan");
		currencies.put("COP", "Colombian Peso");
		currencies.put("CRC", "Costa Rican Colón");
		currencies.put("CUC", "Cuban Convertible Peso");
		currencies.put("CUP", "Cuban Peso");
		currencies.put("CVE", "Cape Verdean Escudo");
		currencies.put("CZK", "Czech Republic Koruna");
		currencies.put("DJF", "Djiboutian Franc");
		currencies.put("DKK", "Danish Krone");
		currencies.put("DOP", "Dominican Peso");
		currencies.put("DZD", "Algerian Dinar");
		currencies.put("EGP", "Egyptian Pound");
		currencies.put("ERN", "Eritrean Nakfa");
		currencies.put("ETB", "Ethiopian Birr");
		currencies.put("EUR", "Euro");
		currencies.put("FJD", "Fijian Dollar");
		currencies.put("FKP", "Falkland Islands Pound");
		currencies.put("GBP", "British Pound Sterling");
		currencies.put("GEL", "Georgian Lari");
		currencies.put("GGP", "Guernsey Pound");
		currencies.put("GHS", "Ghanaian Cedi");
		currencies.put("GIP", "Gibraltar Pound");
		currencies.put("GMD", "Gambian Dalasi");
		currencies.put("GNF", "Guinean Franc");
		currencies.put("GTQ", "Guatemalan Quetzal");
		currencies.put("GYD", "Guyanaese Dollar");
		currencies.put("HKD", "Hong Kong Dollar");
		currencies.put("HNL", "Honduran Lempira");
		currencies.put("HRK", "Croatian Kuna");
		currencies.put("HTG", "Haitian Gourde");
		currencies.put("HUF", "Hungarian Forint");
		currencies.put("IDR", "Indonesian Rupiah");
		currencies.put("ILS", "Israeli New Sheqel");
		currencies.put("IMP", "Manx pound");
		currencies.put("INR", "Indian Rupee");
		currencies.put("IQD", "Iraqi Dinar");
		currencies.put("IRR", "Iranian Rial");
		currencies.put("ISK", "Icelandic Króna");
		currencies.put("JEP", "Jersey Pound");
		currencies.put("JMD", "Jamaican Dollar");
		currencies.put("JOD", "Jordanian Dinar");
		currencies.put("JPY", "Japanese Yen");
		currencies.put("KES", "Kenyan Shilling");
		currencies.put("KGS", "Kyrgystani Som");
		currencies.put("KHR", "Cambodian Riel");
		currencies.put("KMF", "Comorian Franc");
		currencies.put("KPW", "North Korean Won");
		currencies.put("KRW", "South Korean Won");
		currencies.put("KWD", "Kuwaiti Dinar");
		currencies.put("KYD", "Cayman Islands Dollar");
		currencies.put("KZT", "Kazakhstani Tenge");
		currencies.put("LAK", "Laotian Kip");
		currencies.put("LBP", "Lebanese Pound");
		currencies.put("LKR", "Sri Lankan Rupee");
		currencies.put("LRD", "Liberian Dollar");
		currencies.put("LSL", "Lesotho Loti");
		currencies.put("LYD", "Libyan Dinar");
		currencies.put("MAD", "Moroccan Dirham");
		currencies.put("MDL", "Moldovan Leu");
		currencies.put("MGA", "Malagasy Ariary");
		currencies.put("MKD", "Macedonian Denar");
		currencies.put("MMK", "Myanma Kyat");
		currencies.put("MNT", "Mongolian Tugrik");
		currencies.put("MOP", "Macanese Pataca");
		currencies.put("MRO", "Mauritanian Ouguiya (pre-2018)");
		currencies.put("MRU", "Mauritanian Ouguiya");
		currencies.put("MUR", "Mauritian Rupee");
		currencies.put("MVR", "Maldivian Rufiyaa");
		currencies.put("MWK", "Malawian Kwacha");
		currencies.put("MXN", "Mexican Peso");
		currencies.put("MYR", "Malaysian Ringgit");
		currencies.put("MZN", "Mozambican Metical");
		currencies.put("NAD", "Namibian Dollar");
		currencies.put("NGN", "Nigerian Naira");
		currencies.put("NIO", "Nicaraguan Córdoba");
		currencies.put("NOK", "Norwegian Krone");
		currencies.put("NPR", "Nepalese Rupee");
		currencies.put("NZD", "New Zealand Dollar");
		currencies.put("OMR", "Omani Rial");
		currencies.put("PAB", "Panamanian Balboa");
		currencies.put("PEN", "Peruvian Nuevo Sol");
		currencies.put("PGK", "Papua New Guinean Kina");
		currencies.put("PHP", "Philippine Peso");
		currencies.put("PKR", "Pakistani Rupee");
		currencies.put("PLN", "Polish Zloty");
		currencies.put("PYG", "Paraguayan Guarani");
		currencies.put("QAR", "Qatari Rial");
		currencies.put("RON", "Romanian Leu");
		currencies.put("RSD", "Serbian Dinar");
		currencies.put("RUB", "Russian Ruble");
		currencies.put("RWF", "Rwandan Franc");
		currencies.put("SAR", "Saudi Riyal");
		currencies.put("SBD", "Solomon Islands Dollar");
		currencies.put("SCR", "Seychellois Rupee");
		currencies.put("SDG", "Sudanese Pound");
		currencies.put("SEK", "Swedish Krona");
		currencies.put("SGD", "Singapore Dollar");
		currencies.put("SHP", "Saint Helena Pound");
		currencies.put("SLL", "Sierra Leonean Leone");
		currencies.put("SOS", "Somali Shilling");
		currencies.put("SRD", "Surinamese Dollar");
		currencies.put("SSP", "South Sudanese Pound");
		currencies.put("STD", "São Tomé and Príncipe Dobra (pre-2018)");
		currencies.put("STN", "São Tomé and Príncipe Dobra");
		currencies.put("SVC", "Salvadoran Colón");
		currencies.put("SYP", "Syrian Pound");
		currencies.put("SZL", "Swazi Lilangeni");
		currencies.put("THB", "Thai Baht");
		currencies.put("TJS", "Tajikistani Somoni");
		currencies.put("TMT", "Turkmenistani Manat");
		currencies.put("TND", "Tunisian Dinar");
		currencies.put("TOP", "Tongan Pa anga");
		currencies.put("TRY", "Turkish Lira");
		currencies.put("TTD", "Trinidad and Tobago Dollar");
		currencies.put("TWD", "New Taiwan Dollar");
		currencies.put("TZS", "Tanzanian Shilling");
		currencies.put("UAH", "Ukrainian Hryvnia");
		currencies.put("UGX", "Ugandan Shilling");
		currencies.put("USD", "United States Dollar");
		currencies.put("UYU", "Uruguayan Peso");
		currencies.put("UZS", "Uzbekistan Som");
		currencies.put("VEF", "Venezuelan Bolívar Fuerte (Old)");
		currencies.put("VES", "Venezuelan Bolívar Soberano");
		currencies.put("VND", "Vietnamese Dong");
		currencies.put("VUV", "Vanuatu Vatu");
		currencies.put("WST", "Samoan Tala");
		currencies.put("XAF", "CFA Franc BEAC");
		currencies.put("XAG", "Silver Ounce");
		currencies.put("XAU", "Gold Ounce");
		currencies.put("XCD", "East Caribbean Dollar");
		currencies.put("XDR", "Special Drawing Rights");
		currencies.put("XOF", "CFA Franc BCEAO");
		currencies.put("XPD", "Palladium Ounce");
		currencies.put("XPF", "CFP Franc");
		currencies.put("XPT", "Platinum Ounce");
		currencies.put("YER", "Yemeni Rial");
		currencies.put("ZAR", "South African Rand");
		currencies.put("ZMW", "Zambian Kwacha");
		currencies.put("ZWL", "Zimbabwean Dollar");
	}
	private static final class DoubleFilter implements UnaryOperator<Change> {
		@Override
		public Change apply(Change change) {
			String newText = change.getControlNewText();
		    if(newText.trim().isEmpty()) {
		    		change.setText("");
		    		return change;
		    }
		    try {
		    		Double.parseDouble(newText);
		    		return change;
		    } catch (NumberFormatException e) {
			    return null;
			}
		}
	}

	private static final class StringDoubleConverter extends StringConverter<Double> {
		@Override
		public Double fromString(String s) {
			return s.trim().isEmpty() ? null: Double.parseDouble(s);
		}

		@Override
		public String toString(Double d) {
			return d == null ? "" : Double.toString(d);
		}
	}

}
