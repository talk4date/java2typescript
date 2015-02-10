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
package java2typescript.jackson.module.grammar.base;

import java.io.IOException;
import java.io.Writer;

/** Type referenced by its name and capable of writing its own definition */
abstract public class AbstractNamedType extends AbstractType {

	protected final String name;
	
	/**
	 * If this is non-null this is the name to use to reference a type from another position than from the definition.
	 */
	private String nameReferenceOverride;
	
	/**
	 * the full canonical java name.
	 */
	private String canonicalName;

	public AbstractNamedType(String className) {
		this.name = className;
	}

	@Override
	public void write(Writer writer) throws IOException {
		writer.write(nameReferenceOverride != null ? nameReferenceOverride : name);
	}

	public String getName() {
		return name;
	}
	
	public void setNameReferenceOverride(String nameReferenceOverride) {
		this.nameReferenceOverride = nameReferenceOverride;
	}
	
	public String getCanonicalName() {
		return canonicalName;
	}
	
	public void setCanonicalName(String javaPackage) {
		this.canonicalName = javaPackage;
	}

	abstract public void writeDef(Writer writer) throws IOException;
}
