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
 * Annotation for methods that can be invoked as commands
 *
 * @author leo
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
public @interface command {

    /**
     * Description of the option, used to generate a help message of this
     * command-line options.
     */
    String description() default "";

    /**
     * One-line usage note e.g. "$ command name (argument)"
     * @return
     */
    String usage() default "";

    /**
     * Detailed help message of the command. For writing multi-line help messages,
     * "|" can be used as prefixes of lines.
     * @return
     */
    String detailedHelp() default "";


}
