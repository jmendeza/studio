/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.studio.api.v1.to;

import java.io.Serializable;

public class DmError implements Serializable {

    private static final long serialVersionUID = -1276175491559890255L;
    protected String _site;
    protected String _path;
    protected Exception _e;

    public DmError(String site, String path, Exception e) {
        this._site = site;
        this._path = path;
        this._e = e;
    }
    
    public String getSite() {
        return this._site;
    }
    
    public String getPath() {
        return this._path;
    }

    public Exception getException() {
        return this._e;
    }
}
