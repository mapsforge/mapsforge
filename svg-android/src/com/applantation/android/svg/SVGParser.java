/*Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 *
 *Changes Copyright 2011 Google Inc.
 */

package com.applantation.android.svg;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;

import org.kxml2.io.*;
import org.xmlpull.v1.*;

/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

/**
 * Entry point for parsing SVG files for Android.
 * Use one of the various static methods for parsing SVGs by resource, asset or input stream.
 * Optionally, a single color can be searched and replaced in the SVG while parsing.
 * You can also parse an svg path directly.
 *
 * @see #getSVGFromResource(android.content.res.Resources, int)
 * @see #getSVGFromAsset(android.content.res.AssetManager, String)
 * @see #getSVGFromString(String)
 * @see #getSVGFromInputStream(java.io.InputStream)
 * @see #parsePath(String)
 *
 * @author Larva Labs, LLC
 */
public class SVGParser {

	static final String TAG = "SVGAndroid";

	/**
	 * Parse SVG data from an input stream.
	 * @param svgData the input stream, with SVG XML data in UTF-8 character encoding.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromInputStream(InputStream svgData) throws SVGParseException {
		return SVGParser.parse(svgData, 0, 0, false);
	}

	/**
	 * Parse SVG data from a string.
	 * @param svgData the string containing SVG XML data.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromString(String svgData) throws SVGParseException {
		return SVGParser.parse(new ByteArrayInputStream(svgData.getBytes()), 0, 0, false);
	}

	/**
	 * Parse SVG data from an Android application resource.
	 * @param resources the Android context resources.
	 * @param resId the ID of the raw resource SVG.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromResource(Resources resources, int resId) throws SVGParseException {
		return SVGParser.parse(resources.openRawResource(resId), 0, 0, false);
	}

	/**
	 * Parse SVG data from an Android application asset.
	 * @param assetMngr the Android asset manager.
	 * @param svgPath the path to the SVG file in the application's assets.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 * @throws IOException if there was a problem reading the file.
	 */
	public static SVG getSVGFromAsset(AssetManager assetMngr, String svgPath) throws SVGParseException, IOException {
		InputStream inputStream = assetMngr.open(svgPath);
		SVG svg = getSVGFromInputStream(inputStream);
		inputStream.close();
		return svg;
	}

	/**
	 * Parse SVG data from an input stream, replacing a single color with another color.
	 * @param svgData the input stream, with SVG XML data in UTF-8 character encoding.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromInputStream(InputStream svgData, int searchColor, int replaceColor) throws SVGParseException {
		return SVGParser.parse(svgData, searchColor, replaceColor, false);
	}

	/**
	 * Parse SVG data from a string.
	 * @param svgData the string containing SVG XML data.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromString(String svgData, int searchColor, int replaceColor) throws SVGParseException {
		return SVGParser.parse(new ByteArrayInputStream(svgData.getBytes()), searchColor, replaceColor, false);
	}

	/**
	 * Parse SVG data from an Android application resource.
	 * @param resources the Android context
	 * @param resId the ID of the raw resource SVG.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 */
	public static SVG getSVGFromResource(Resources resources, int resId, int searchColor, int replaceColor) throws SVGParseException {
		return SVGParser.parse(resources.openRawResource(resId), searchColor, replaceColor, false);
	}

	/**
	 * Parse SVG data from an Android application asset.
	 * @param assetMngr the Android asset manager.
	 * @param svgPath the path to the SVG file in the application's assets.
	 * @param searchColor the color in the SVG to replace.
	 * @param replaceColor the color with which to replace the search color.
	 * @return the parsed SVG.
	 * @throws SVGParseException if there is an error while parsing.
	 * @throws IOException if there was a problem reading the file.
	 */
	public static SVG getSVGFromAsset(AssetManager assetMngr, String svgPath, int searchColor, int replaceColor) throws SVGParseException, IOException {
		InputStream inputStream = assetMngr.open(svgPath);
		SVG svg = getSVGFromInputStream(inputStream, searchColor, replaceColor);
		inputStream.close();
		return svg;
	}

	/**
	 * Parses a single SVG path and returns it as a <code>android.graphics.Path</code> object.
	 * An example path is <code>M250,150L150,350L350,350Z</code>, which draws a triangle.
	 *
	 * @param pathString the SVG path, see the specification <a href="http://www.w3.org/TR/SVG/paths.html">here</a>.
	 */
	public static Path parsePath(String pathString) {
		return doPath(pathString);
	}

	private static SVG parse(InputStream in, Integer searchColor, Integer replaceColor, boolean whiteMode) throws SVGParseException {
		SVGHandler svgHandler = null;
		try {
			final Picture picture = new Picture();

			XmlPullParser xr = new KXmlParser();
			xr.setInput(new InputStreamReader(in));

			svgHandler = new SVGHandler(xr, picture);
			svgHandler.setColorSwap(searchColor, replaceColor);
			svgHandler.setWhiteMode(whiteMode);
			svgHandler.processSvg();
			
			SVG result = new SVG(picture, svgHandler.bounds);
			if (!Float.isInfinite(svgHandler.limits.top)) {
				result.setLimits(svgHandler.limits);
			}
			return result;
		} catch (Exception e) {
			throw new SVGParseException(e);
		}
	}
	

	private static NumberParse parseNumbers(String s) {
		//Util.debug("Parsing numbers from: '" + s + "'");
		int n = s.length();
		int p = 0;
		ArrayList<Float> numbers = new ArrayList<Float>();
		boolean skipChar = false;
		for (int i = 1; i < n; i++) {
			if (skipChar) {
				skipChar = false;
				continue;
			}
			char c = s.charAt(i);
			switch (c) {
			// This ends the parsing, as we are on the next element
			case 'M':
			case 'm':
			case 'Z':
			case 'z':
			case 'L':
			case 'l':
			case 'H':
			case 'h':
			case 'V':
			case 'v':
			case 'C':
			case 'c':
			case 'S':
			case 's':
			case 'Q':
			case 'q':
			case 'T':
			case 't':
			case 'a':
			case 'A':
			case ')': {
				String str = s.substring(p, i);
				if (str.trim().length() > 0) {
					//Util.debug("  Last: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
				}
				p = i;
				return new NumberParse(numbers, p);
			}
			case '\n':
			case '\t':
			case ' ':
			case ',': {
				String str = s.substring(p, i);
				// Just keep moving if multiple whitespace
				if (str.trim().length() > 0) {
					//Util.debug("  Next: " + str);
					Float f = Float.parseFloat(str);
					numbers.add(f);
					if (c == '-') {
						p = i;
					} else {
						p = i + 1;
						skipChar = true;
					}
				} else {
					p++;
				}
				break;
			}
			}
		}
		String last = s.substring(p);
		if (last.length() > 0) {
			//Util.debug("  Last: " + last);
			try {
				numbers.add(Float.parseFloat(last));
			} catch (NumberFormatException nfe) {
				// Just white-space, forget it
			}
			p = s.length();
		}
		return new NumberParse(numbers, p);
	}

	// Process a list of transforms
	// foo(n,n,n...) bar(n,n,n..._ ...)
	// delims are whitespace or ,'s

	private static Matrix parseTransform(String s) {
		Matrix matrix = new Matrix();
		while (true) {
			parseTransformItem(s, matrix);
			// Log.i(TAG, "Transformed: (" + s + ") " + matrix);
			int rparen = s.indexOf(")");
			if (rparen > 0 && s.length() > rparen + 1) {
				s = s.substring(rparen + 1).replaceFirst("[\\s,]*", "");
			} else {
				break;
			}
		}
		//Log.d(TAG, matrix.toShortString());
		return matrix;
	}

	private static Matrix parseTransformItem(String s, Matrix matrix) {
		if (s.startsWith("matrix(")) {
			NumberParse np = parseNumbers(s.substring("matrix(".length()));
			if (np.numbers.size() == 6) {
				Matrix mat = new Matrix();
				mat.setValues(new float[] {
						// Row 1
						np.numbers.get(0),
						np.numbers.get(2),
						np.numbers.get(4),
						// Row 2
						np.numbers.get(1),
						np.numbers.get(3),
						np.numbers.get(5),
						// Row 3
						0,
						0,
						1,
				});
				matrix.preConcat(mat);
			}
		} else if (s.startsWith("translate(")) {
			NumberParse np = parseNumbers(s.substring("translate(".length()));
			if (np.numbers.size() > 0) {
				float tx = np.numbers.get(0);
				float ty = 0;
				if (np.numbers.size() > 1) {
					ty = np.numbers.get(1);
				}
				matrix.preTranslate(tx, ty);
			}
		} else if (s.startsWith("scale(")) {
			NumberParse np = parseNumbers(s.substring("scale(".length()));
			if (np.numbers.size() > 0) {
				float sx = np.numbers.get(0);
				float sy = sx;
				if (np.numbers.size() > 1) {
					sy = np.numbers.get(1);
				}
				matrix.preScale(sx, sy);
			}
		} else if (s.startsWith("skewX(")) {
			NumberParse np = parseNumbers(s.substring("skewX(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew((float) Math.tan(angle), 0);
			}
		} else if (s.startsWith("skewY(")) {
			NumberParse np = parseNumbers(s.substring("skewY(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				matrix.preSkew(0, (float) Math.tan(angle));
			}
		} else if (s.startsWith("rotate(")) {
			NumberParse np = parseNumbers(s.substring("rotate(".length()));
			if (np.numbers.size() > 0) {
				float angle = np.numbers.get(0);
				float cx = 0;
				float cy = 0;
				if (np.numbers.size() > 2) {
					cx = np.numbers.get(1);
					cy = np.numbers.get(2);
				}
				matrix.preTranslate(cx, cy);
				matrix.preRotate(angle);
				matrix.preTranslate(-cx, -cy);
			}
		} else {
			Log.i(TAG, "Invalid transform (" + s + ")");
		}
		return matrix;
	}

	/**
	 * This is where the hard-to-parse paths are handled.
	 * Uppercase rules are absolute positions, lowercase are relative.
	 * Types of path rules:
	 * <p/>
	 * <ol>
	 * <li>M/m - (x y)+ - Move to (without drawing)
	 * <li>Z/z - (no params) - Close path (back to starting point)
	 * <li>L/l - (x y)+ - Line to
	 * <li>H/h - x+ - Horizontal ine to
	 * <li>V/v - y+ - Vertical line to
	 * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
	 * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1, y1 of this bezier)
	 * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
	 * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t. to current point)
	 * </ol>
	 * <p/>
	 * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a - sign)
	 *
	 * @param s the path string from the XML
	 */
	private static Path doPath(String s) {
		int n = s.length();
		ParserHelper ph = new ParserHelper(s, 0);
		ph.skipWhitespace();
		Path p = new Path();
		float lastX = 0;
		float lastY = 0;
		float lastX1 = 0;
		float lastY1 = 0;
		RectF r = new RectF();
		char cmd = 'x';
		while (ph.pos < n) {
			char next = s.charAt(ph.pos);
			if (!Character.isDigit(next) && !(next == '.') && !(next == '-')) {
				cmd = next;
				ph.advance();
			} else if (cmd == 'M') { // implied command
				cmd = 'L';
			} else if (cmd == 'm') { // implied command
				cmd = 'l';
			} else { // implied command
				// Log.d(TAG, "Implied command: " + cmd);
			}
			p.computeBounds(r, true);
			// Log.d(TAG, "  " + cmd + " " + r);
			// Util.debug("* Commands remaining: '" + path + "'.");
			boolean wasCurve = false;
			switch (cmd) {
			case 'M':
			case 'm': {
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'm') {
					p.rMoveTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					p.moveTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'Z':
			case 'z': {
				p.close();
				break;
			}
			case 'L':
			case 'l': {
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'l') {
					p.rLineTo(x, y);
					lastX += x;
					lastY += y;
				} else {
					p.lineTo(x, y);
					lastX = x;
					lastY = y;
				}
				break;
			}
			case 'H':
			case 'h': {
				float x = ph.nextFloat();
				if (cmd == 'h') {
					p.rLineTo(x, 0);
					lastX += x;
				} else {
					p.lineTo(x, lastY);
					lastX = x;
				}
				break;
			}
			case 'V':
			case 'v': {
				float y = ph.nextFloat();
				if (cmd == 'v') {
					p.rLineTo(0, y);
					lastY += y;
				} else {
					p.lineTo(lastX, y);
					lastY = y;
				}
				break;
			}
			case 'C':
			case 'c': {
				wasCurve = true;
				float x1 = ph.nextFloat();
				float y1 = ph.nextFloat();
				float x2 = ph.nextFloat();
				float y2 = ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'c') {
					x1 += lastX;
					x2 += lastX;
					x += lastX;
					y1 += lastY;
					y2 += lastY;
					y += lastY;
				}
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'S':
			case 's': {
				wasCurve = true;
				float x2 = ph.nextFloat();
				float y2 = ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 's') {
					x2 += lastX;
					x += lastX;
					y2 += lastY;
					y += lastY;
				}
				float x1 = 2 * lastX - lastX1;
				float y1 = 2 * lastY - lastY1;
				p.cubicTo(x1, y1, x2, y2, x, y);
				lastX1 = x2;
				lastY1 = y2;
				lastX = x;
				lastY = y;
				break;
			}
			case 'A':
			case 'a': {
				float rx = ph.nextFloat();
				float ry = ph.nextFloat();
				float theta = ph.nextFloat();
				int largeArc = (int) ph.nextFloat();
				int sweepArc = (int) ph.nextFloat();
				float x = ph.nextFloat();
				float y = ph.nextFloat();
				if (cmd == 'a') {
					x += lastX;
					y += lastY;
				}
				drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc == 1, sweepArc == 1);
				lastX = x;
				lastY = y;
				break;
			}
			default:
				Log.d(TAG, "Invalid path command: " + cmd);
				ph.advance();
			}
			if (!wasCurve) {
				lastX1 = lastX;
				lastY1 = lastY;
			}
			ph.skipWhitespace();
		}
		return p;
	}

	/**
	 * Elliptical arc implementation based on the SVG specification notes
	 * Adapted from the Batik library (Apache-2 license) by SAU
	 */

	private static void drawArc(Path path, double x0, double y0, double x, double y, double rx,
			double ry, double angle, boolean largeArcFlag, boolean sweepFlag) {
		double dx2 = (x0 - x) / 2.0;
		double dy2 = (y0 - y) / 2.0;
		angle = Math.toRadians(angle % 360.0);
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);

		double x1 = (cosAngle * dx2 + sinAngle * dy2);
		double y1 = (-sinAngle * dx2 + cosAngle * dy2);
		rx = Math.abs(rx);
		ry = Math.abs(ry);

		double Prx = rx * rx;
		double Pry = ry * ry;
		double Px1 = x1 * x1;
		double Py1 = y1 * y1;

		// check that radii are large enough
		double radiiCheck = Px1 / Prx + Py1 / Pry;
		if (radiiCheck > 1) {
			rx = Math.sqrt(radiiCheck) * rx;
			ry = Math.sqrt(radiiCheck) * ry;
			Prx = rx * rx;
			Pry = ry * ry;
		}

		// Step 2 : Compute (cx1, cy1)
		double sign = (largeArcFlag == sweepFlag) ? -1 : 1;
		double sq = ((Prx * Pry) - (Prx * Py1) - (Pry * Px1))
		/ ((Prx * Py1) + (Pry * Px1));
		sq = (sq < 0) ? 0 : sq;
		double coef = (sign * Math.sqrt(sq));
		double cx1 = coef * ((rx * y1) / ry);
		double cy1 = coef * -((ry * x1) / rx);

		double sx2 = (x0 + x) / 2.0;
		double sy2 = (y0 + y) / 2.0;
		double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
		double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);

		// Step 4 : Compute the angleStart (angle1) and the angleExtent (dangle)
		double ux = (x1 - cx1) / rx;
		double uy = (y1 - cy1) / ry;
		double vx = (-x1 - cx1) / rx;
		double vy = (-y1 - cy1) / ry;
		double p, n;

		// Compute the angle start
		n = Math.sqrt((ux * ux) + (uy * uy));
		p = ux; // (1 * ux) + (0 * uy)
		sign = (uy < 0) ? -1.0 : 1.0;
		double angleStart = Math.toDegrees(sign * Math.acos(p / n));

		// Compute the angle extent
		n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
		p = ux * vx + uy * vy;
		sign = (ux * vy - uy * vx < 0) ? -1.0 : 1.0;
		double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
		if (!sweepFlag && angleExtent > 0) {
			angleExtent -= 360f;
		} else if (sweepFlag && angleExtent < 0) {
			angleExtent += 360f;
		}
		angleExtent %= 360f;
		angleStart %= 360f;

		RectF oval = new RectF((float) (cx - rx), (float) (cy - ry), (float) (cx + rx), (float) (cy + ry));
		path.addArc(oval, (float) angleStart, (float) angleExtent);
	}

	private static NumberParse getNumberParseAttr(String name, XmlPullParser xpp) {
		int n = xpp.getAttributeCount();
		for (int i = 0; i < n; i++) {
			if (xpp.getAttributeName(i).equals(name)) {
				return parseNumbers(xpp.getAttributeValue(i));
			}
		}
		return null;
	}

	private static String getStringAttr(String name, XmlPullParser xpp) {
		int n = xpp.getAttributeCount();
		for (int i = 0; i < n; i++) {
			if (xpp.getAttributeName(i).equals(name)) {
				return xpp.getAttributeValue(i);
			}
		}
		return null;
	}

	private static Float getFloatAttr(String name, XmlPullParser xpp) {
		return getFloatAttr(name, xpp, null);
	}

	private static Float getFloatAttr(String name, XmlPullParser xpp, Float defaultValue) {
		String v = getStringAttr(name, xpp);
		if (v == null) {
			return defaultValue;
		} else {
			if (v.endsWith("px")) {
				v = v.substring(0, v.length() - 2);
			}
			if (v.endsWith("pt")) {
				v = v.substring(0, v.length() - 2);
			}
			return Float.parseFloat(v);
		}
	}
		
	private static class NumberParse {
		private ArrayList<Float> numbers;
		private int nextCmd;

		public NumberParse(ArrayList<Float> numbers, int nextCmd) {
			this.numbers = numbers;
			this.nextCmd = nextCmd;
		}

		@SuppressWarnings("unused")
		public int getNextCmd() {
			return nextCmd;
		}

		@SuppressWarnings("unused")
		public float getNumber(int index) {
			return numbers.get(index);
		}

	}

	private static class Gradient {
		String id;
		String xlink;
		boolean isLinear;
		float x1, y1, x2, y2;
		float x, y, radius;
		ArrayList<Float> positions = new ArrayList<Float>();
		ArrayList<Integer> colors = new ArrayList<Integer>();
		Matrix matrix = null;

		public Gradient createChild(Gradient g) {
			Gradient child = new Gradient();
			child.id = g.id;
			child.xlink = id;
			child.isLinear = g.isLinear;
			child.x1 = g.x1;
			child.x2 = g.x2;
			child.y1 = g.y1;
			child.y2 = g.y2;
			child.x = g.x;
			child.y = g.y;
			child.radius = g.radius;
			child.positions = positions;
			child.colors = colors;
			child.matrix = matrix;
			if (g.matrix != null) {
				if (matrix == null) {
					child.matrix = g.matrix;
				} else {
					Matrix m = new Matrix(matrix);
					m.preConcat(g.matrix);
					child.matrix = m;
				}
			}
			return child;
		}
	}

	private static class StyleSet {
		HashMap<String, String> styleMap = new HashMap<String, String>();

		private StyleSet(String string) {
			String[] styles = string.split(";");
			for (String s : styles) {
				String[] style = s.split(":");
				if (style.length == 2) {
					styleMap.put(style[0], style[1]);
				}
			}
		}

		public String getStyle(String name) {
			return styleMap.get(name);
		}
	}

	private static class Properties {
		StyleSet styles = null;
		XmlPullParser xpp;

		private Properties(XmlPullParser xpp) {
			this.xpp = xpp;
			String styleAttr = getStringAttr("style", xpp);
			if (styleAttr != null) {
				styles = new StyleSet(styleAttr);
			}
		}

		public String getAttr(String name) {
			String v = null;
			if (styles != null) {
				v = styles.getStyle(name);
			}
			if (v == null) {
				v = getStringAttr(name, xpp);
			}
			return v;
		}

		public String getString(String name) {
			return getAttr(name);
		}

		public Integer getColorValue(String name) {
			String v = getAttr(name);
			if (v == null) {
				return null;
			} else if (v.startsWith("#") && (v.length() == 4 || v.length() == 7)) {
				try {
					int result = Integer.parseInt(v.substring(1), 16);
					return v.length() == 4 ? hex3Tohex6(result) : result;
				} catch (NumberFormatException nfe) {
					return null;
				}
			} else {
				return SVGColors.mapColor(v);
			}
		}

		// convert 0xRGB into 0xRRGGBB
		private int hex3Tohex6(int x) {
			return  (x & 0xF00) << 8 | (x & 0xF00) << 12 |
			(x & 0xF0) << 4 | (x & 0xF0) << 8 |
			(x & 0xF) << 4 | (x & 0xF);
		}

		@SuppressWarnings("unused")
		public Float getFloat(String name, float defaultValue) {
			Float v = getFloat(name);
			if (v == null) {
				return defaultValue;
			} else {
				return v;
			}
		}

		public Float getFloat(String name) {
			String v = getAttr(name);
			if (v == null) {
				return null;
			} else {
				try {
					return Float.parseFloat(v);
				} catch (NumberFormatException nfe) {
					return null;
				}
			}
		}
	}
	
	private static class SVGHandler {
		
		XmlPullParser xpp;
		Picture picture;
		Canvas canvas;

		Paint strokePaint;
		boolean strokeSet = false;
		Stack<Paint> strokePaintStack = new Stack<Paint>();
		Stack<Boolean> strokeSetStack = new Stack<Boolean>();

		Paint fillPaint;
		boolean fillSet = false;
		Stack<Paint> fillPaintStack = new Stack<Paint>();
		Stack<Boolean> fillSetStack = new Stack<Boolean>();

		// Scratch rect (so we aren't constantly making new ones)
		RectF rect = new RectF();
		RectF bounds = null;
		RectF limits = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

		Integer searchColor = null;
		Integer replaceColor = null;

		boolean whiteMode = false;

		int pushed = 0;

		private boolean hidden = false;
		private int hiddenLevel = 0;
		private boolean boundsMode = false;
		
		HashMap<String, Shader> gradientMap = new HashMap<String, Shader>();
		HashMap<String, Gradient> gradientRefMap = new HashMap<String, Gradient>();
		Gradient gradient = null;
		
		private boolean inDefsElement = false;

		private SVGHandler(XmlPullParser xpp, Picture picture) {
			this.picture = picture;
			this.xpp = xpp;
			strokePaint = new Paint();
			strokePaint.setAntiAlias(true);
			strokePaint.setStyle(Paint.Style.STROKE);
			fillPaint = new Paint();
			fillPaint.setAntiAlias(true);
			fillPaint.setStyle(Paint.Style.FILL);
		}

		
		public void processSvg() throws XmlPullParserException, IOException {
			int eventType = xpp.getEventType();
			do {
				if(eventType == XmlPullParser.START_DOCUMENT) {
					// no op
				} else if(eventType == XmlPullParser.END_DOCUMENT) {
					// no op
				} else if(eventType == XmlPullParser.START_TAG) {
					startElement();
				} else if(eventType == XmlPullParser.END_TAG) {
					endElement();
				} else if(eventType == XmlPullParser.TEXT) {
					// not implemented
				}
				eventType = xpp.next();
			} while (eventType != XmlPullParser.END_DOCUMENT);
		}

		
		public void setColorSwap(Integer searchColor, Integer replaceColor) {
			this.searchColor = searchColor;
			this.replaceColor = replaceColor;
		}

		public void setWhiteMode(boolean whiteMode) {
			this.whiteMode = whiteMode;
		}

		private boolean doFill(Properties atts, HashMap<String, Shader> gradients) {
			if ("none".equals(atts.getString("display"))) {
				return false;
			}
			if (whiteMode) {
				fillPaint.setShader(null);
				fillPaint.setColor(Color.WHITE);
				return true;
			}
			String fillString = atts.getString("fill");
			if (fillString != null) {
				if (fillString.startsWith("url(#")) {
					// It's a gradient fill, look it up in our map
					String id = fillString.substring("url(#".length(), fillString.length() - 1);
					Shader shader = gradients.get(id);
					if (shader != null) {
						fillPaint.setShader(shader);
						return true;
					} else {
						Log.d(TAG, "Didn't find shader, using black: " + id);
						fillPaint.setShader(null);
						doColor(atts, Color.BLACK, true, fillPaint);
						return true;
					}
				} else if (fillString.equalsIgnoreCase("none")) {
					fillPaint.setShader(null);
					fillPaint.setColor(Color.TRANSPARENT);
					return true;
				} else {
					fillPaint.setShader(null);
					Integer color = atts.getColorValue("fill");
					if (color != null) {
						doColor(atts, color, true, fillPaint);
						return true;
					} else {
						Log.d(TAG, "Unrecognized fill color, using black: " + fillString);
						doColor(atts, Color.BLACK, true, fillPaint);
						return true;
					}
				}
			} else {
				if (fillSet) {
					// If fill is set, inherit from parent
					return fillPaint.getColor() != Color.TRANSPARENT;   // optimization
				} else {
					// Default is black fill
					fillPaint.setShader(null);
					fillPaint.setColor(Color.BLACK);
					return true;
				}
			}
		}


		private boolean doStroke(Properties atts) {
			if (whiteMode) {
				// Never stroke in white mode
				return false;
			}
			if ("none".equals(atts.getString("display"))) {
				return false;
			}

			// Check for other stroke attributes
			Float width = atts.getFloat("stroke-width");
			if (width != null) {
				strokePaint.setStrokeWidth(width);
			}
			
			String linecap = atts.getString("stroke-linecap");
			if ("round".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.ROUND);
			} else if ("square".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.SQUARE);
			} else if ("butt".equals(linecap)) {
				strokePaint.setStrokeCap(Paint.Cap.BUTT);
			}
			
			String linejoin = atts.getString("stroke-linejoin");
			if ("miter".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.MITER);
			} else if ("round".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.ROUND);
			} else if ("bevel".equals(linejoin)) {
				strokePaint.setStrokeJoin(Paint.Join.BEVEL);
			}
			
			pathStyleHelper(atts.getString("stroke-dasharray"), atts.getString("stroke-dashoffset"));
			
			String strokeString = atts.getAttr("stroke");
			if (strokeString != null) {
				if (strokeString.equalsIgnoreCase("none")) {
					strokePaint.setColor(Color.TRANSPARENT);
					return false;
				} else {
					Integer color = atts.getColorValue("stroke");
					if (color != null) {
						doColor(atts, color, false, strokePaint);
						return true;
					} else {
						Log.d(TAG, "Unrecognized stroke color, using none: " + strokeString);
						strokePaint.setColor(Color.TRANSPARENT);
						return false;
					}
				}
			} else {
				if (strokeSet) {
					// Inherit from parent
					return strokePaint.getColor() != Color.TRANSPARENT;   // optimization					
				} else {
					// Default is none
					strokePaint.setColor(Color.TRANSPARENT);
					return false;
				}
			}
		}

		private Gradient doGradient(boolean isLinear, XmlPullParser xpp) {
			Gradient gradient = new Gradient();
			gradient.id = getStringAttr("id", xpp);
			gradient.isLinear = isLinear;
			if (isLinear) {
				gradient.x1 = getFloatAttr("x1", xpp, 0f);
				gradient.x2 = getFloatAttr("x2", xpp, 0f);
				gradient.y1 = getFloatAttr("y1", xpp, 0f);
				gradient.y2 = getFloatAttr("y2", xpp, 0f);
			} else {
				gradient.x = getFloatAttr("cx", xpp, 0f);
				gradient.y = getFloatAttr("cy", xpp, 0f);
				gradient.radius = getFloatAttr("r", xpp, 0f);
			}
			String transform = getStringAttr("gradientTransform", xpp);
			if (transform != null) {
				gradient.matrix = parseTransform(transform);
			}
			String xlink = getStringAttr("href", xpp);
			if (xlink != null) {
				if (xlink.startsWith("#")) {
					xlink = xlink.substring(1);
				}
				gradient.xlink = xlink;
			}
			return gradient;
		}

		private void doColor(Properties atts, Integer color, boolean fillMode, Paint paint) {
			int c = (0xFFFFFF & color) | 0xFF000000;
			if (searchColor != null && searchColor.intValue() == c) {
				c = replaceColor;
			}
			paint.setColor(c);
			Float opacity = atts.getFloat("opacity");
			if (opacity == null) {
				opacity = atts.getFloat(fillMode ? "fill-opacity" : "stroke-opacity");
			}
			if (opacity == null) {
				paint.setAlpha(255);
			} else {
				paint.setAlpha((int) (255 * opacity));
			}
		}

		/**
		 * set the path style (if any)
		 *  stroke-dasharray="n1,n2,..."
		 *  stroke-dashoffset=n
		 */

		private void pathStyleHelper(String style, String offset) {
			if (style == null) {
				return;
			}
			
			if (style.equals("none")) {
				strokePaint.setPathEffect(null);
				return;
			}
			
			StringTokenizer st = new StringTokenizer(style, " ,");
			int count = st.countTokens();
			float[] intervals = new float[(count&1) == 1 ? count * 2 : count];
			float max = 0;
			float current = 1f;
			int i = 0;
			while(st.hasMoreTokens()) {
				intervals[i++] = current = toFloat(st.nextToken(), current);
				max += current;
			}
			
			// in svg speak, we double the intervals on an odd count
			for (int start=0; i < intervals.length; i++, start++) {
				max += intervals[i] = intervals[start];
			}

			float off = 0f;
			if (offset != null) {
				try {
					off = Float.parseFloat(offset) % max;
				} catch (NumberFormatException e) {
					// ignore
				}
			}
			
			strokePaint.setPathEffect(new DashPathEffect(intervals, off));
		}

		private static float toFloat(String s, float dflt) {
			float result = dflt;
			try {
				result = Float.parseFloat(s);
			} catch (NumberFormatException e) {
				// ignore
			}
			return result;
		}

		private void doLimits(float x, float y) {
			if (x < limits.left) {
				limits.left = x;
			}
			if (x > limits.right) {
				limits.right = x;
			}
			if (y < limits.top) {
				limits.top = y;
			}
			if (y > limits.bottom) {
				limits.bottom = y;
			}
		}

		private void doLimits(float x, float y, float width, float height) {
			doLimits(x, y);
			doLimits(x + width, y + height);
		}

		private void doLimits(Path path) {
			path.computeBounds(rect, false);
			doLimits(rect.left, rect.top);
			doLimits(rect.right, rect.bottom);
		}

		private final static Matrix IDENTITY_MATRIX = new Matrix();

		private void pushTransform(XmlPullParser xpp) {
			final String transform = getStringAttr("transform", xpp);
			final Matrix matrix = transform == null ? IDENTITY_MATRIX : parseTransform(transform);
			pushed++;
			canvas.save(); 	
			canvas.concat(matrix);
		}

		private void popTransform() {
			canvas.restore();
			//Log.d(TAG, "matrix pop: " + canvas.getMatrix());
			pushed--;
		}
		
		public void startElement() {
		    String localName = xpp.getName();
    
		    strokePaint.setAlpha(255);
			fillPaint.setAlpha(255);
	
			// Ignore everything but rectangles in bounds mode
			if (boundsMode) {
				if (localName.equals("rect")) {
					Float x = getFloatAttr("x", xpp);
					if (x == null) {
						x = 0f;
					}
					Float y = getFloatAttr("y", xpp);
					if (y == null) {
						y = 0f;
					}
					Float width = getFloatAttr("width", xpp);
					Float height = getFloatAttr("height", xpp);
					bounds = new RectF(x, y, x + width, y + height);
				}
				return;
			}
			
			if (inDefsElement) {
				return;
			}
			
			if (localName.equals("svg")) {
				int width = (int) Math.ceil(getFloatAttr("width", xpp));
				int height = (int) Math.ceil(getFloatAttr("height", xpp));
				NumberParse viewbox = getNumberParseAttr("viewBox", xpp);
				canvas = picture.beginRecording(width, height);
				if (viewbox != null && viewbox.numbers != null && viewbox.numbers.size() == 4) {
					float sx = width / (viewbox.numbers.get(2) - viewbox.numbers.get(0)) ;
					float sy = height / (viewbox.numbers.get(3) - viewbox.numbers.get(1));					
					canvas.scale(sx, sy);
				}
			} else if (localName.equals("defs")) {
				inDefsElement = true;
			} else if (localName.equals("linearGradient")) {
				gradient = doGradient(true, xpp);
			} else if (localName.equals("radialGradient")) {
				gradient = doGradient(false, xpp);
			} else if (localName.equals("stop")) {
				if (gradient != null) {
					float offset = getFloatAttr("offset", xpp);
					String styles = getStringAttr("style", xpp);
					StyleSet styleSet = new StyleSet(styles);
					String colorStyle = styleSet.getStyle("stop-color");
					int color = Color.BLACK;
					if (colorStyle != null) {
						if (colorStyle.startsWith("#")) {
							color = Integer.parseInt(colorStyle.substring(1), 16);
						} else {
							color = Integer.parseInt(colorStyle, 16);
						}
					}
					String opacityStyle = styleSet.getStyle("stop-opacity");
					if (opacityStyle != null) {
						float alpha = Float.parseFloat(opacityStyle);
						int alphaInt = Math.round(255 * alpha);
						color |= (alphaInt << 24);
					} else {
						color |= 0xFF000000;
					}
					gradient.positions.add(offset);
					gradient.colors.add(color);
				}
			} else if (localName.equals("g")) {
				// Check to see if this is the "bounds" layer
				if ("bounds".equalsIgnoreCase(getStringAttr("id", xpp))) {
					boundsMode = true;
				}
				if (hidden) {
					hiddenLevel++;
					//Util.debug("Hidden up: " + hiddenLevel);
				}
				// Go in to hidden mode if display is "none"
				if ("none".equals(getStringAttr("display", xpp))) {
					if (!hidden) {
						hidden = true;
						hiddenLevel = 1;
						//Util.debug("Hidden up: " + hiddenLevel);
					}
				}
				pushTransform(xpp); 
				Properties props = new Properties(xpp);

				fillPaintStack.push(new Paint(fillPaint));
				strokePaintStack.push(new Paint(strokePaint));
				fillSetStack.push(fillSet);
				strokeSetStack.push(strokeSet);

				doFill(props, gradientMap);
				doStroke(props);
				
				fillSet |= (props.getString("fill") != null);
				strokeSet |= (props.getString("stroke") != null);
			} else if (!hidden && localName.equals("rect")) {
				Float x = getFloatAttr("x", xpp);
				if (x == null) {
					x = 0f;
				}
				Float y = getFloatAttr("y", xpp);
				if (y == null) {
					y = 0f;
				}
				Float width = getFloatAttr("width", xpp);
				Float height = getFloatAttr("height", xpp);
				Float rx = getFloatAttr("rx", xpp, 0f);
				Float ry = getFloatAttr("ry", xpp, 0f);
				pushTransform(xpp);
				Properties props = new Properties(xpp);
				if (doFill(props, gradientMap)) {
					doLimits(x, y, width, height);
					if (rx <= 0f && ry <= 0f) {
						canvas.drawRect(x, y, x + width, y + height, fillPaint);
					} else {
						rect.set(x, y, x + width, y + height);
						canvas.drawRoundRect(rect, rx, ry, fillPaint);
					}
				}
				if (doStroke(props)) {
					if (rx <= 0f && ry <= 0f) {
						canvas.drawRect(x, y, x + width, y + height, strokePaint);
					} else {
						rect.set(x, y, x + width, y + height);
						canvas.drawRoundRect(rect, rx, ry, strokePaint);
					}
				}
				popTransform();
			} else if (!hidden && localName.equals("line")) {
				Float x1 = getFloatAttr("x1", xpp);
				Float x2 = getFloatAttr("x2", xpp);
				Float y1 = getFloatAttr("y1", xpp);
				Float y2 = getFloatAttr("y2", xpp);
				Properties props = new Properties(xpp);
				if (doStroke(props)) {
					pushTransform(xpp);
					doLimits(x1, y1);
					doLimits(x2, y2);
					canvas.drawLine(x1, y1, x2, y2, strokePaint);
					popTransform();
				}
			} else if (!hidden && localName.equals("circle")) {
				Float centerX = getFloatAttr("cx", xpp);
				Float centerY = getFloatAttr("cy", xpp);
				Float radius = getFloatAttr("r", xpp);
				if (centerX != null && centerY != null && radius != null) {
					pushTransform(xpp);
					Properties props = new Properties(xpp);
					if (doFill(props, gradientMap)) {
						doLimits(centerX - radius, centerY - radius);
						doLimits(centerX + radius, centerY + radius);
						canvas.drawCircle(centerX, centerY, radius, fillPaint);
					}
					if (doStroke(props)) {
						canvas.drawCircle(centerX, centerY, radius, strokePaint);
					}
					popTransform();
				}
			} else if (!hidden && localName.equals("ellipse")) {
				Float centerX = getFloatAttr("cx", xpp);
				Float centerY = getFloatAttr("cy", xpp);
				Float radiusX = getFloatAttr("rx", xpp);
				Float radiusY = getFloatAttr("ry", xpp);
				if (centerX != null && centerY != null && radiusX != null && radiusY != null) {
					pushTransform(xpp);
					Properties props = new Properties(xpp);
					rect.set(centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);
					if (doFill(props, gradientMap)) {
						doLimits(centerX - radiusX, centerY - radiusY);
						doLimits(centerX + radiusX, centerY + radiusY);
						canvas.drawOval(rect, fillPaint);
					}
					if (doStroke(props)) {
						canvas.drawOval(rect, strokePaint);
					}
					popTransform();
				}
			} else if (!hidden && (localName.equals("polygon") || localName.equals("polyline"))) {
				NumberParse numbers = getNumberParseAttr("points", xpp);
				if (numbers != null) {
					Path p = new Path();
					ArrayList<Float> points = numbers.numbers;
					if (points.size() > 1) {
						pushTransform(xpp);
						Properties props = new Properties(xpp);
						p.moveTo(points.get(0), points.get(1));
						for (int i = 2; i < points.size(); i += 2) {
							float x = points.get(i);
							float y = points.get(i + 1);
							p.lineTo(x, y);
						}
						// Don't close a polyline
						if (localName.equals("polygon")) {
							p.close();
						}
						if (doFill(props, gradientMap)) {
							doLimits(p);

							// showBounds("fill", p);
							canvas.drawPath(p, fillPaint);
						}
						if (doStroke(props)) {
							// showBounds("stroke", p);
							canvas.drawPath(p, strokePaint);
						}
						popTransform();
					}
				}
			} else if (!hidden && localName.equals("path")) {
				Path p = doPath(getStringAttr("d", xpp));
				pushTransform(xpp);
				Properties props = new Properties(xpp);
				if (doFill(props, gradientMap)) {
					// showBounds("gradient", p);
					doLimits(p);
					// showBounds("gradient", p);
					canvas.drawPath(p, fillPaint);
				}
				if (doStroke(props)) {
					// showBounds("paint", p);
					canvas.drawPath(p, strokePaint);
				}
				popTransform();
			} else if (!hidden) {
				Log.d(TAG, "UNRECOGNIZED SVG COMMAND: " + localName);
			}
		}

		@SuppressWarnings("unused")
		private void showBounds(String text, Path p) {
			RectF b= new RectF();
			p.computeBounds(b, true);
			Log.d(TAG, text + " bounds: " + b.left + "," + b.bottom + " to " + b.right + "," + b.top);
		}

		@SuppressWarnings("unused")
		private String showAttributes(XmlPullParser xpp) {
			String result = "";
			for(int i=0; i < xpp.getAttributeCount() ; i++) {
				result += " " + xpp.getAttributeName(i) + "='" + xpp.getAttributeValue(i) + "'";
			}
			return result;
		}

		public void endElement() {
	        String localName = xpp.getName();

			if (inDefsElement) {
				if (localName.equals("defs")) {
					inDefsElement = false;
				}
				return;
			}
			
			if (localName.equals("svg")) {
				picture.endRecording();
			} else if (localName.equals("linearGradient")) {
				if (gradient.id != null) {
					if (gradient.xlink != null) {
						Gradient parent = gradientRefMap.get(gradient.xlink);
						if (parent != null) {
							gradient = parent.createChild(gradient);
						}
					}
					int[] colors = new int[gradient.colors.size()];
					for (int i = 0; i < colors.length; i++) {
						colors[i] = gradient.colors.get(i);
					}
					float[] positions = new float[gradient.positions.size()];
					for (int i = 0; i < positions.length; i++) {
						positions[i] = gradient.positions.get(i);
					}
					if (colors.length == 0) {
						//.d("BAD", "BAD");
					}
					LinearGradient g = new LinearGradient(gradient.x1, gradient.y1, gradient.x2, gradient.y2, colors, positions, Shader.TileMode.CLAMP);
					if (gradient.matrix != null) {
						g.setLocalMatrix(gradient.matrix);
					}
					gradientMap.put(gradient.id, g);
					gradientRefMap.put(gradient.id, gradient);
				}
			} else if (localName.equals("radialGradient")) {
				if (gradient.id != null) {
					int[] colors = new int[gradient.colors.size()];
					for (int i = 0; i < colors.length; i++) {
						colors[i] = gradient.colors.get(i);
					}
					float[] positions = new float[gradient.positions.size()];
					for (int i = 0; i < positions.length; i++) {
						positions[i] = gradient.positions.get(i);
					}
					if (gradient.xlink != null) {
						Gradient parent = gradientRefMap.get(gradient.xlink);
						if (parent != null) {
							gradient = parent.createChild(gradient);
						}
					}
					RadialGradient g = new RadialGradient(gradient.x, gradient.y, gradient.radius, colors, positions, Shader.TileMode.CLAMP);
					if (gradient.matrix != null) {
						g.setLocalMatrix(gradient.matrix);
					}
					gradientMap.put(gradient.id, g);
					gradientRefMap.put(gradient.id, gradient);
				}
			} else if (localName.equals("g")) {
				if (boundsMode) {
					boundsMode = false;
				}
				// Break out of hidden mode
				if (hidden) {
					hiddenLevel--;
					//Util.debug("Hidden down: " + hiddenLevel);
					if (hiddenLevel == 0) {
						hidden = false;
					}
				}
				// Clear gradient map
				gradientMap.clear();
				popTransform(); // SAU
				fillPaint = fillPaintStack.pop();
				fillSet = fillSetStack.pop();
				strokePaint = strokePaintStack.pop();
				strokeSet = strokeSetStack.pop();
			}
		}
	}
}

