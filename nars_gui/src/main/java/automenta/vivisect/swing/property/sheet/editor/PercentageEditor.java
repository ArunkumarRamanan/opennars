/**
 * L2FProd Common v9.2 License.
 *
 * Copyright 2005 - 2009 L2FProd.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package automenta.vivisect.swing.property.sheet.editor;

import automenta.vivisect.swing.property.propertysheet.Property;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * Percentage editor.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class PercentageEditor extends SpinnerEditor {

	/**
	 * Percentage editor. Argument has to be an object so javax.bean API handles
	 * it correctly.
	 * 
	 * @param property the property object (instance of {@link Property})
	 */
	public PercentageEditor(Object property) {
		super();

		if (!(property instanceof Property)) {
			throw new IllegalArgumentException(String.format("Property has to be %s instance. Instead found %s", Property.class, property.getClass()));
		}

		Property prop = (Property) property;
		Class<?> type = prop.getType();

		int pstart = 0;
		int pmin = 0;
		int pmax = 100;
		int pstep = 1;

		Number start = null;
		Comparable<?> min = null;
		Comparable<?> max = null;
		Number step = null;

		//noinspection IfStatementWithTooManyBranches
		if (type == Byte.class || type == byte.class) {
			start = (byte) pstart;
			min = (byte) pmin;
			max = (byte) pmax;
			step = (byte) pstep;
		} else if (type == Short.class || type == short.class) {
			start = (short) pstart;
			min = (short) pmin;
			max = (short) pmax;
			step = (short) pstep;
		} else if (type == Integer.class || type == int.class) {
			start = pstart;
			min = pmin;
			max = pmax;
			step = pstep;
		} else if (type == Long.class || type == long.class) {
			start = (long) pstart;
			min = (long) pmin;
			max = (long) pmax;
			step = (long) pstep;
		} else if (type == Float.class || type == float.class) {
			start = (float) pstart;
			min = (float) pmin;
			max = (float) pmax;
			step = (float) pstep;
		} else if (type == Double.class || type == double.class) {
			start = (double) pstart;
			min = (double) pmin;
			max = (double) pmax;
			step = (double) pstep;
		} else if (type == BigDecimal.class) {
			start = new BigDecimal(pstart);
			min = new BigDecimal(pmin);
			max = new BigDecimal(pmax);
			step = new BigDecimal(pstep);
		} else if (type == BigInteger.class) {
			start = new BigInteger(Integer.toString(pstart), 10);
			min = new BigInteger(Integer.toString(pmin), 10);
			max = new BigInteger(Integer.toString(pmax), 10);
			step = new BigInteger(Integer.toString(pstep), 10);
		}

		SpinnerModel model = new SpinnerNumberModel(start, min, max, step);

		spinner.setModel(model);

		formatSpinner();
	}
}
