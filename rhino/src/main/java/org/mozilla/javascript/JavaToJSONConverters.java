/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.function.UnaryOperator;

/**
 * This class provides implementations of converters for Java objects to be used by the
 * JSON.stringify method.
 *
 * <p>JSON.stringify will automatically convert instances of java.util.Map to javascript objects.
 * Instances of java.util.Collection and java Arrays will be converted to javascript arrays.
 *
 * <p>This is a final effort at conversion for other java objects that appear as values, and may be
 * preempted by objects which define a toJSON() method or by a replacer function passed to
 * JSON.stringify. The return value will, in turn, be converted according to {@link
 * Context#javaToJS} and stringified.
 *
 * @author Tony Germano
 */
public class JavaToJSONConverters {

    private JavaToJSONConverters() {}

    /** Convert Object to its toString() value. */
    public static final UnaryOperator<Object> STRING = o -> o.toString();

    /** Always return undefined */
    public static final UnaryOperator<Object> UNDEFINED = o -> Undefined.instance;

    /** Always return an empty object */
    public static final UnaryOperator<Object> EMPTY_OBJECT = o -> Collections.EMPTY_MAP;

    /** Throw a TypeError naming the class that could not be converted */
    public static final UnaryOperator<Object> THROW_TYPE_ERROR =
            o -> {
                throw ScriptRuntime.typeErrorById(
                        "msg.json.cant.serialize", o.getClass().getName());
            };

    /**
     * Convert JavaBean to an object as long as it has at least one readable property
     *
     * <p>If unable to determine properties or if none exist, null is returned. This method can be
     * called from other converters to provide an alternate value on a returned null.
     */
    public static final UnaryOperator<Object> BEAN =
            value -> {
                LinkedHashMap<String, Object> properties = new LinkedHashMap<>();
                Method[] methods = value.getClass().getMethods();
                for (Method method : methods) {
                    if (isGetter(method)) {
                        String propertyName = getPropertyName(method);
                        Object propValue;
                        try {
                            propValue = method.invoke(value);
                        } catch (Exception e) {
                            continue;
                        }
                        properties.put(propertyName, propValue);
                    }
                }

                if (properties.isEmpty()) return null;

                LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
                obj.put("beanClass", value.getClass().getName());
                obj.put("properties", properties);
                return obj;
            };

    private static boolean isGetter(Method method) {
        if (!method.getName().startsWith("get")) return false;
        if (method.getParameterTypes().length != 0) return false;
        if (void.class.equals(method.getReturnType())) return false;
        return true;
    }

    private static String getPropertyName(Method method) {
        String name = method.getName().substring(3);
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
