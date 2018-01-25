/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.fastr;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import static com.oracle.truffle.r.runtime.RVisibility.ON;

import com.oracle.truffle.r.runtime.ResourceHandlerFactory;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FastRHelp {

    @RBuiltin(name = ".fastr.interop.helpPath", visibility = ON, kind = PRIMITIVE, parameterNames = {"builtinName"}, behavior = COMPLEX)
    public abstract static class FastRHelpPath extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FastRHelpPath.class);
            casts.arg("builtinName").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization()
        @TruffleBoundary
        public Object helpPath(String builtinName) {
            String path = "/com/oracle/truffle/r/nodes/builtin/base/Rd/" + builtinName + ".Rd";
            try (InputStream in = ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), path)) {
                if (in != null) {
                    return path;
                }
            } catch (IOException ex) {
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.interop.helpRd", visibility = ON, kind = PRIMITIVE, parameterNames = {"path"}, behavior = COMPLEX)
    public abstract static class FastRHelpRd extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(FastRHelpRd.class);
            casts.arg("path").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization()
        @TruffleBoundary
        public Object getHelpRdPath(String path) {
            try (InputStream in = ResourceHandlerFactory.getHandler().getResourceAsStream(getClass(), path)) {
                if (in != null) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        return sb.toString();
                    }
                }
            } catch (IOException ex) {
                RError.warning(this, RError.Message.GENERIC, "problems while reading " + path, ex.getMessage());
            }
            return RNull.instance;
        }
    }
}
