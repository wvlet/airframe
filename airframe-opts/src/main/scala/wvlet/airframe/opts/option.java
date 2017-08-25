/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wvlet.airframe.opts;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying command-line options.
 *
 * @author leo
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
public @interface option {

    /**
     * Comma-separated list of option prefixes. For example, "-h,--help" handles option "-h" and
     * "--help". If no prefix is specified, this parameter is handled as a nested option.
     */
    String prefix() default "";

    /**
     * Description of the option, used to generate a help message of this
     * command-line option.
     */
    String description() default "";

    /**
     * If this option is used as a switch to display help messages of commands, set this value to true.
     */
    boolean isHelp() default false;
}


