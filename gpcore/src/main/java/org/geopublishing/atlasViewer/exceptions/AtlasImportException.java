/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Tzeggai.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Tzeggai - initial API and implementation
 ******************************************************************************/
package org.geopublishing.atlasViewer.exceptions;


public class AtlasImportException extends AtlasException {

	public AtlasImportException() {
		super(); 
	}

	public AtlasImportException(String message, Throwable cause) {
		super(message, cause);
	}

	public AtlasImportException(String message) {
		super(message);
	}

	public AtlasImportException(Throwable cause) {
		super(cause);
	}

}
