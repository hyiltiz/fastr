/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi.interop;

import static com.oracle.truffle.r.runtime.ffi.interop.UnsafeAdapter.UNSAFE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;

import sun.misc.Unsafe;

public final class NativeDoubleArray extends NativeArray<double[]> {

    public NativeDoubleArray(double[] value) {
        super(value);
    }

    double read(int index) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            return UNSAFE.getDouble(nativeAddress + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        } else {
            return array[index];
        }
    }

    void write(int index, double nv) {
        long nativeAddress = nativeAddress();
        if (nativeAddress != 0) {
            UNSAFE.putDouble(nativeAddress + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, nv);
        } else {
            array[index] = nv;
        }
    }

    @Override
    @TruffleBoundary
    protected long allocateNative() {
        long nativeAddress = UNSAFE.allocateMemory(array.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        UNSAFE.copyMemory(array, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, null, nativeAddress, array.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        return nativeAddress;
    }

    @Override
    @TruffleBoundary
    protected void copyBackFromNative(long nativeAddress) {
        // copy back
        UNSAFE.copyMemory(null, nativeAddress, array, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, array.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return NativeDoubleArrayMRForeign.ACCESS;
    }
}
