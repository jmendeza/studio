/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.studio.impl.v2.security.password;

import org.springframework.lang.NonNull;

/**
 * Factory to create a task to handle the forgot password request
 *
 * @since 4.1.2
 */
public interface ForgotPasswordTaskFactory {
    /**
     * Creates a task to be executed to handle the forgot password request
     *
     * @param username the username
     * @return the task
     */
    @NonNull
    Runnable prepareTask(@NonNull String username);
}
