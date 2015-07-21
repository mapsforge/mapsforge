/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
 * Copyright 2014 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.rendertheme.rule;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import org.kxml2.io.KXmlParser;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.renderinstruction.Area;
import org.mapsforge.map.rendertheme.renderinstruction.Caption;
import org.mapsforge.map.rendertheme.renderinstruction.Circle;
import org.mapsforge.map.rendertheme.renderinstruction.Line;
import org.mapsforge.map.rendertheme.renderinstruction.LineSymbol;
import org.mapsforge.map.rendertheme.renderinstruction.PathText;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforge.map.rendertheme.renderinstruction.Symbol;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * KXML handler to parse XML render theme files.
 */
public final class RenderThemeHandler {

	private static enum Element {
		RENDER_THEME, RENDERING_INSTRUCTION, RULE, RENDERING_STYLE;
	}
	private static final Logger LOGGER = Logger.getLogger(RenderThemeHandler.class.getName());
	private static final String ELEMENT_NAME_RULE = "rule";
	private static final String UNEXPECTED_ELEMENT = "unexpected element: ";

	public static RenderTheme getRenderTheme(GraphicFactory graphicFactory, DisplayModel displayModel,
			XmlRenderTheme xmlRenderTheme) throws IOException, XmlPullParserException {
		XmlPullParser pullParser = new KXmlParser();

		RenderThemeHandler renderThemeHandler = new RenderThemeHandler(graphicFactory, displayModel,
				xmlRenderTheme.getRelativePathPrefix(), xmlRenderTheme, pullParser);
		InputStream inputStream = null;
		try {
			inputStream = xmlRenderTheme.getRenderThemeAsStream();
			pullParser.setInput(inputStream, null);
			renderThemeHandler.processRenderTheme();
			return renderThemeHandler.renderTheme;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private Set<String> categories;
	private Rule currentRule;
	private final DisplayModel displayModel;
	private final Stack<Element> elementStack = new Stack<Element>();
	private final GraphicFactory graphicFactory;
	private int level;
	private final XmlPullParser pullParser;
	private String qName;
	private final String relativePathPrefix;
	private RenderTheme renderTheme;
	private final Stack<Rule> ruleStack = new Stack<Rule>();
	private Map<String, Symbol> symbols = new HashMap<String, Symbol>();
	private final XmlRenderTheme xmlRenderTheme;
	private XmlRenderThemeStyleMenu renderThemeStyleMenu;
	private XmlRenderThemeStyleLayer currentLayer;

	private RenderThemeHandler(GraphicFactory graphicFactory, DisplayModel displayModel, String relativePathPrefix,
	                           XmlRenderTheme xmlRenderTheme, XmlPullParser pullParser) {
		super();
		this.pullParser = pullParser;
		this.graphicFactory = graphicFactory;
		this.displayModel = displayModel;
		this.relativePathPrefix = relativePathPrefix;
		this.xmlRenderTheme = xmlRenderTheme;
	}


	public void processRenderTheme() throws XmlPullParserException, IOException {
		int eventType = pullParser.getEventType();
		do {
			if(eventType == XmlPullParser.START_DOCUMENT) {
				// no-op
			} else if(eventType == XmlPullParser.START_TAG) {
				startElement();
			} else if(eventType == XmlPullParser.END_TAG) {
				endElement();
			} else if(eventType == XmlPullParser.TEXT) {
				// not implemented
			}
			eventType = pullParser.next();
		} while (eventType != XmlPullParser.END_DOCUMENT);
		endDocument();
	}


	private void endDocument() {
		if (this.renderTheme == null) {
			throw new IllegalArgumentException("missing element: rules");
		}

		this.renderTheme.setLevels(this.level);
		this.renderTheme.complete();
	}

	private void endElement() {
		qName = pullParser.getName();

		this.elementStack.pop();

		if (ELEMENT_NAME_RULE.equals(qName)) {
			this.ruleStack.pop();
			if (this.ruleStack.empty()) {
				if (isVisible(this.currentRule)) {
					this.renderTheme.addRule(this.currentRule);
				}
			} else {
				this.currentRule = this.ruleStack.peek();
			}
		}
		else if ("stylemenu".equals(qName)) {
			// when we are finished parsing the menu part of the file, we can get the
			// categories to render from the initiator. This allows the creating action
			// to select which of the menu options to choose
			if (null != this.xmlRenderTheme.getMenuCallback()) {
				// if there is no callback, there is no menu, so the categories will be null
				this.categories = this.xmlRenderTheme.getMenuCallback().getCategories(this.renderThemeStyleMenu);
			}
			return;
		}

	}

	private void startElement() throws XmlPullParserException {
		qName = pullParser.getName();

		try {
			if ("rendertheme".equals(qName)) {
				checkState(qName, Element.RENDER_THEME);
				this.renderTheme = new RenderThemeBuilder(this.graphicFactory, qName, pullParser).build();
			}

			else if (ELEMENT_NAME_RULE.equals(qName)) {
				checkState(qName, Element.RULE);
				Rule rule = new RuleBuilder(qName, pullParser, this.ruleStack).build();
				if (!this.ruleStack.empty() && isVisible(rule)) {
					this.currentRule.addSubRule(rule);
				}
				this.currentRule = rule;
				this.ruleStack.push(this.currentRule);
			}

			else if ("area".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Area area = new Area(this.graphicFactory, this.displayModel, qName, pullParser, this.level++,
						this.relativePathPrefix);
				if (isVisible(area)) {
					this.currentRule.addRenderingInstruction(area);
				}
			}

			else if ("caption".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Caption caption = new Caption(this.graphicFactory, this.displayModel, qName, pullParser, symbols);
				if (isVisible(caption)) {
					this.currentRule.addRenderingInstruction(caption);
				}
			}

			else if ("cat".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				this.currentLayer.addCategory(getStringAttribute("id"));
			}

			else if ("circle".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Circle circle = new Circle(this.graphicFactory, this.displayModel, qName, pullParser,
						this.level++);
				if (isVisible(circle)) {
					this.currentRule.addRenderingInstruction(circle);
				}
			}

			// rendertheme menu layer
			else if ("layer".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				boolean enabled = false;
				if (getStringAttribute("enabled") != null) {
					enabled = Boolean.valueOf(getStringAttribute("enabled"));
				}
				boolean visible = Boolean.valueOf(getStringAttribute("visible"));
				this.currentLayer = this.renderThemeStyleMenu.createLayer(getStringAttribute("id"), visible, enabled);
				String parent = getStringAttribute("parent");
				if (null != parent) {
					XmlRenderThemeStyleLayer parentEntry = this.renderThemeStyleMenu.getLayer(parent);
					if (null != parentEntry) {
						for (String cat : parentEntry.getCategories()) {
							this.currentLayer.addCategory(cat);
						}
						for (XmlRenderThemeStyleLayer overlay : parentEntry.getOverlays()) {
							this.currentLayer.addOverlay(overlay);
						}
					}
				}
			}

			else if ("line".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Line line = new Line(this.graphicFactory, this.displayModel, qName, pullParser, this.level++,
						this.relativePathPrefix);
				if (isVisible(line)) {
					this.currentRule.addRenderingInstruction(line);
				}
			}

			else if ("lineSymbol".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				LineSymbol lineSymbol = new LineSymbol(this.graphicFactory, this.displayModel, qName,
						pullParser, this.relativePathPrefix);
				if (isVisible(lineSymbol)) {
					this.currentRule.addRenderingInstruction(lineSymbol);
				}
			}

			// render theme menu name
			else if ("name".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				this.currentLayer.addTranslation(getStringAttribute("lang"), getStringAttribute("value"));
			}

			// render theme menu overlay
			else if ("overlay".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				XmlRenderThemeStyleLayer overlay = this.renderThemeStyleMenu.getLayer(getStringAttribute("id"));
				if (overlay != null) {
					this.currentLayer.addOverlay(overlay);
				}
			}

			else if ("pathText".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				PathText pathText = new PathText(this.graphicFactory, this.displayModel, qName, pullParser);
				if (isVisible(pathText)) {
					this.currentRule.addRenderingInstruction(pathText);
				}
			}

			else if ("stylemenu".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);

				this.renderThemeStyleMenu =
						new XmlRenderThemeStyleMenu(getStringAttribute("id"),
								getStringAttribute("defaultlang"), getStringAttribute("defaultvalue"));
			}

			else if ("symbol".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Symbol symbol = new Symbol(this.graphicFactory, this.displayModel, qName, pullParser,
						this.relativePathPrefix);
				this.currentRule.addRenderingInstruction(symbol);
				String symbolId = symbol.getId();
				if (symbolId != null) {
					this.symbols.put(symbolId, symbol);
				}
			}

			else {
				throw new XmlPullParserException("unknown element: " + qName);
			}
		} catch (IOException e) {
			LOGGER.warning("Rendertheme missing or invalid resource " + e.getMessage());
		}
	}

	private void checkElement(String elementName, Element element) throws XmlPullParserException {
		switch (element) {
			case RENDER_THEME:
				if (!this.elementStack.empty()) {
					throw new XmlPullParserException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RULE:
				Element parentElement = this.elementStack.peek();
				if (parentElement != Element.RENDER_THEME && parentElement != Element.RULE) {
					throw new XmlPullParserException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RENDERING_INSTRUCTION:
				if (this.elementStack.peek() != Element.RULE) {
					throw new XmlPullParserException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RENDERING_STYLE:
				return;
		}

		throw new XmlPullParserException("unknown enum value: " + element);
	}

	private void checkState(String elementName, Element element) throws XmlPullParserException {
		checkElement(elementName, element);
		this.elementStack.push(element);
	}

	private String getStringAttribute(String name) {
		int n = pullParser.getAttributeCount();
		for (int i = 0; i < n; i++) {
			if (pullParser.getAttributeName(i).equals(name)) {
				return pullParser.getAttributeValue(i);
			}
		}
		return null;
	}

	private boolean isVisible(RenderInstruction renderInstruction) {
		return this.categories == null || renderInstruction.getCategory() == null ||
				this.categories.contains(renderInstruction.getCategory());
	}

	private boolean isVisible(Rule rule) {
		// a rule is visible if categories is not set, the rule has not category or the
		// categories contain this rule's category
		return this.categories == null || rule.cat == null || this.categories.contains(rule.cat);
	}
}
