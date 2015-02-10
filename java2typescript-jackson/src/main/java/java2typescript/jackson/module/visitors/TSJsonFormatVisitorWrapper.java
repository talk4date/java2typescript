/*******************************************************************************
 * Copyright 2013 Raphael Jolivet
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package java2typescript.jackson.module.visitors;

import java2typescript.jackson.module.grammar.ClassType;
import java2typescript.jackson.module.grammar.EnumType;
import java2typescript.jackson.module.grammar.Module;
import java2typescript.jackson.module.grammar.base.AbstractNamedType;
import java2typescript.jackson.module.grammar.base.AbstractType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitable;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonIntegerFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonMapFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNullFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonNumberFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonStringFormatVisitor;

public class TSJsonFormatVisitorWrapper extends ABaseTSJsonFormatVisitor implements JsonFormatVisitorWrapper {

	public TSJsonFormatVisitorWrapper(ABaseTSJsonFormatVisitor parentHolder) {
		super(parentHolder);
	}

	public TSJsonFormatVisitorWrapper(Module module) {
		super(module);
	}

	private <T extends ABaseTSJsonFormatVisitor<?>> T setTypeAndReturn(T actualVisitor) {
		type = actualVisitor.getType();
		return actualVisitor;
	}

	/** Visit recursively the type, or return a cached response */
	public static AbstractType getTSTypeForHandler(ABaseTSJsonFormatVisitor<?> baseVisitor,
			JsonFormatVisitable handler, JavaType typeHint) throws JsonMappingException {

		AbstractType computedType = baseVisitor.getComputedTypes().get(typeHint);

		if (computedType != null) {
			return computedType;
		}

		TSJsonFormatVisitorWrapper visitor = new TSJsonFormatVisitorWrapper(baseVisitor);
		handler.acceptJsonFormatVisitor(visitor, typeHint);
		baseVisitor.getComputedTypes().put(typeHint, visitor.getType());
		return visitor.getType();
	}

	/** Either Java simple name or @JsonTypeName annotation */
	private String getName(JavaType type) {
		String pkgName = type.getRawClass().getPackage().getName();
		String prefix = pkgName.substring(pkgName.lastIndexOf(".") +1, pkgName.length()) + ".";
		
		JsonTypeName typeName = type.getRawClass().getAnnotation(JsonTypeName.class);
		if (typeName != null) {
			return prefix + typeName.value();
		} else {
			return prefix + type.getRawClass().getSimpleName();
		}
	}

	private TSJsonObjectFormatVisitor useNamedClassOrParse(JavaType javaType) {

		String name = getName(javaType);

		AbstractNamedType namedType = getModule().getNamedTypes().get(name);

		if (namedType == null) {
			TSJsonObjectFormatVisitor visitor = new TSJsonObjectFormatVisitor(this, name, javaType.getRawClass());
			ClassType classType = visitor.getType();
			classType.setCanonicalName(javaType.getRawClass().getCanonicalName());
			type = classType;
			
			getModule().getNamedTypes().put(visitor.getType().getName(), visitor.getType());
			return visitor;
		} else {
			type = namedType;
			return null;
		}
	}

	private EnumType parseEnumOrGetFromCache(JavaType javaType) {
		String name = getName(javaType);
		AbstractType namedType = getModule().getNamedTypes().get(name);
		if (namedType == null) {
			EnumType enumType = new EnumType(name);
			for (Object val : javaType.getRawClass().getEnumConstants()) {
				enumType.getValues().add(val.toString());
			}
			enumType.setCanonicalName(javaType.getRawClass().getCanonicalName());
			getModule().getNamedTypes().put(name, enumType);
			return enumType;
		} else {
			return (EnumType) namedType;
		}
	}

	@Override
	public JsonObjectFormatVisitor expectObjectFormat(JavaType type) throws JsonMappingException {
		return useNamedClassOrParse(type);
	}

	@Override
	public JsonArrayFormatVisitor expectArrayFormat(JavaType type) throws JsonMappingException {
		return setTypeAndReturn(new TSJsonArrayFormatVisitor(this));
	}

	@Override
	public JsonStringFormatVisitor expectStringFormat(JavaType jType) throws JsonMappingException {
		// also serialize enums, but reference them as strings
		if(jType.getRawClass().isEnum()) {
			parseEnumOrGetFromCache(jType);
		}
		return setTypeAndReturn(new TSJsonStringFormatVisitor(this));
	}

	@Override
	public JsonNumberFormatVisitor expectNumberFormat(JavaType type) throws JsonMappingException {
		return setTypeAndReturn(new TSJsonNumberFormatVisitor(this));
	}

	@Override
	public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) throws JsonMappingException {
		return setTypeAndReturn(new TSJsonNumberFormatVisitor(this));
	}

	@Override
	public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type) throws JsonMappingException {
		return setTypeAndReturn(new TSJsonBooleanFormatVisitor(this));
	}

	@Override
	public JsonNullFormatVisitor expectNullFormat(JavaType type) throws JsonMappingException {
		return setTypeAndReturn(new TSJsonNullFormatVisitor(this));
	}

	@Override
	public JsonAnyFormatVisitor expectAnyFormat(JavaType type) throws JsonMappingException {
		return setTypeAndReturn(new TSJsonAnyFormatVisitor(this));
	}

	@Override
	public JsonMapFormatVisitor expectMapFormat(JavaType type) throws JsonMappingException {
		return setTypeAndReturn(new TSJsonMapFormatVisitor(this));
	}

}
