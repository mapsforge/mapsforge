/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.renderinstruction.Area;
import org.mapsforge.map.rendertheme.renderinstruction.AreaBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.Caption;
import org.mapsforge.map.rendertheme.renderinstruction.CaptionBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.CaptionedSymbol;
import org.mapsforge.map.rendertheme.renderinstruction.CaptionedSymbolBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.Circle;
import org.mapsforge.map.rendertheme.renderinstruction.CircleBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.Line;
import org.mapsforge.map.rendertheme.renderinstruction.LineBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.LineSymbol;
import org.mapsforge.map.rendertheme.renderinstruction.LineSymbolBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.PathText;
import org.mapsforge.map.rendertheme.renderinstruction.PathTextBuilder;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforge.map.rendertheme.renderinstruction.Symbol;
import org.mapsforge.map.rendertheme.renderinstruction.SymbolBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 handler to parse XML render theme files.
 */
public final class RenderThemeHandler extends DefaultHandler {

	private static enum Element {
		RENDER_THEME, RENDERING_INSTRUCTION, RULE, RENDERING_STYLE;
	}

	private static final String ELEMENT_NAME_RULE = "rule";
	private static final Logger LOGGER = Logger.getLogger(RenderThemeHandler.class.getName());
	private static final String UNEXPECTED_ELEMENT = "unexpected element: ";

	public static RenderTheme getRenderTheme(GraphicFactory graphicFactory, DisplayModel displayModel,
			XmlRenderTheme xmlRenderTheme) throws SAXException, ParserConfigurationException, IOException {
		RenderThemeHandler renderThemeHandler = new RenderThemeHandler(graphicFactory, displayModel,
				xmlRenderTheme.getRelativePathPrefix(), xmlRenderTheme);
		XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		xmlReader.setContentHandler(renderThemeHandler);
		InputStream inputStream = null;
		try {
			inputStream = xmlRenderTheme.getRenderThemeAsStream();
			xmlReader.parse(new InputSource(inputStream));
			renderThemeHandler.renderTheme.incrementRefCount();
			return renderThemeHandler.renderTheme;
		} finally {
			if (renderThemeHandler.renderTheme != null) {
				renderThemeHandler.renderTheme.destroy();
			}
			IOUtils.closeQuietly(inputStream);
		}
	}

	private Set<String> categories;
	private Rule currentRule;
	private final DisplayModel displayModel;
	private final Stack<Element> elementStack = new Stack<Element>();
	private final GraphicFactory graphicFactory;
	private int level;
	private final String relativePathPrefix;
	private RenderTheme renderTheme;
	private final Stack<Rule> ruleStack = new Stack<Rule>();
	private final XmlRenderTheme xmlRenderTheme;
	private XmlRenderThemeStyleMenu renderThemeStyleMenu;
	private XmlRenderThemeStyleLayer currentLayer;

	private RenderThemeHandler(GraphicFactory graphicFactory, DisplayModel displayModel, String relativePathPrefix,
	                           XmlRenderTheme xmlRenderTheme) {
		super();
		this.graphicFactory = graphicFactory;
		this.displayModel = displayModel;
		this.relativePathPrefix = relativePathPrefix;
		this.xmlRenderTheme = xmlRenderTheme;
	}

	@Override
	public void endDocument() {
		if (this.renderTheme == null) {
			throw new IllegalArgumentException("missing element: rules");
		}

		this.renderTheme.setLevels(this.level);
		this.renderTheme.complete();
	}

	@Override
	public void endElement(String uri, String localName, String qName) {

		this.elementStack.pop();

		if (ELEMENT_NAME_RULE.equals(qName)) {
			this.ruleStack.pop();
			if (this.ruleStack.empty()) {
				if (isVisible((this.currentRule))) {
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
				// if there is no callback, there is no menu, so we categories will be null
				this.categories = this.xmlRenderTheme.getMenuCallback().getCategories(this.renderThemeStyleMenu);
			}
			return;
		}

	}

	@Override
	public void error(SAXParseException exception) {
		LOGGER.log(Level.SEVERE, null, exception);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		try {
			if ("rendertheme".equals(qName)) {
				checkState(qName, Element.RENDER_THEME);
				this.renderTheme = new RenderThemeBuilder(this.graphicFactory, qName, attributes).build();
			}

			else if (ELEMENT_NAME_RULE.equals(qName)) {
				checkState(qName, Element.RULE);
				Rule rule = new RuleBuilder(qName, attributes, this.ruleStack).build();
				if (!this.ruleStack.empty()) {
					if (isVisible(rule)) {
						this.currentRule.addSubRule(rule);
					}
				}
				this.currentRule = rule;
				this.ruleStack.push(this.currentRule);
			}

			else if ("area".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Area area = new AreaBuilder(this.graphicFactory, this.displayModel, qName, attributes, this.level++,
						this.relativePathPrefix).build();
				if (isVisible(area)) {
					this.currentRule.addRenderingInstruction(area);
				}
			}

			else if ("caption".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Caption caption = new CaptionBuilder(this.graphicFactory, this.displayModel, qName, attributes).build();
				if (isVisible(caption)) {
					this.currentRule.addRenderingInstruction(caption);
				}
			}

			else if ("captioned-symbol".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				CaptionedSymbol captionedSymbol = new CaptionedSymbolBuilder(this.graphicFactory, this.displayModel, qName, attributes, relativePathPrefix).build();
				if (isVisible(captionedSymbol)) {
					this.currentRule.addRenderingInstruction(captionedSymbol);
				}
			}

			else if ("cat".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				this.currentLayer.addCategory(attributes.getValue("id"));
			}

			else if ("circle".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Circle circle = new CircleBuilder(this.graphicFactory, this.displayModel, qName, attributes,
						this.level++).build();
				if (isVisible(circle)) {
					this.currentRule.addRenderingInstruction(circle);
				}
			}

			// rendertheme menu layer
			else if ("layer".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				boolean enabled = false;
				if (attributes.getValue("enabled") != null) {
					enabled = Boolean.valueOf(attributes.getValue("enabled"));
				}
				boolean visible = Boolean.valueOf(attributes.getValue("visible"));
				this.currentLayer = this.renderThemeStyleMenu.createLayer(attributes.getValue("id"), visible, enabled);
				String parent = attributes.getValue("parent");
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
				Line line = new LineBuilder(this.graphicFactory, this.displayModel, qName, attributes, this.level++,
						this.relativePathPrefix).build();
				if (isVisible(line)) {
					this.currentRule.addRenderingInstruction(line);
				}
			}

			else if ("lineSymbol".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				LineSymbol lineSymbol = new LineSymbolBuilder(this.graphicFactory, this.displayModel, qName,
						attributes, this.relativePathPrefix).build();
				if (isVisible(lineSymbol)) {
					this.currentRule.addRenderingInstruction(lineSymbol);
				}
			}

			// render theme menu name
			else if ("name".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				this.currentLayer.addTranslation(attributes.getValue("lang"), attributes.getValue("value"));
			}

			// render theme menu overlay
			else if ("overlay".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);
				XmlRenderThemeStyleLayer overlay = this.renderThemeStyleMenu.getLayer(attributes.getValue("id"));
				if (overlay != null) {
					this.currentLayer.addOverlay(overlay);
				}
			}

			else if ("pathText".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				PathText pathText = new PathTextBuilder(this.graphicFactory, this.displayModel, qName, attributes)
						.build();
				if (isVisible(pathText)) {
					this.currentRule.addRenderingInstruction(pathText);
				}
			}

			else if ("stylemenu".equals(qName)) {
				checkState(qName, Element.RENDERING_STYLE);

				this.renderThemeStyleMenu =
						new XmlRenderThemeStyleMenu(attributes.getValue("id"),
								attributes.getValue("defaultlang"), attributes.getValue("defaultvalue"));
			}

			else if ("symbol".equals(qName)) {
				checkState(qName, Element.RENDERING_INSTRUCTION);
				Symbol symbol = new SymbolBuilder(this.graphicFactory, this.displayModel, qName, attributes,
						this.relativePathPrefix).build();
				this.currentRule.addRenderingInstruction(symbol);
			}

			else {
				throw new SAXException("unknown element: " + qName);
			}
		} catch (IOException e) {
			LOGGER.warning("Rendertheme missing or invalid resource " + e.getMessage());
		}
	}

	@Override
	public void warning(SAXParseException exception) {
		LOGGER.log(Level.SEVERE, null, exception);
	}

	private void checkElement(String elementName, Element element) throws SAXException {
		switch (element) {
			case RENDER_THEME:
				if (!this.elementStack.empty()) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RULE:
				Element parentElement = this.elementStack.peek();
				if (parentElement != Element.RENDER_THEME && parentElement != Element.RULE) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RENDERING_INSTRUCTION:
				if (this.elementStack.peek() != Element.RULE) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RENDERING_STYLE:
				return;
		}

		throw new SAXException("unknown enum value: " + element);
	}

	private void checkState(String elementName, Element element) throws SAXException {
		checkElement(elementName, element);
		this.elementStack.push(element);
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
