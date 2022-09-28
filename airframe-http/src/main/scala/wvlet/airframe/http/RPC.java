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

package wvlet.airframe.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation for defining RPC interfaces.
 * All public methods inside the class marked with `@RPC` will be HTTP endpoints.
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface RPC {
    /**
     * An uri beginning from / (slash).
     *
     * If no path is specified, the full class path name and function names will be used for defining HTTP endpoint path.
     * For example, `/example.myapp.(class name)/(method name)`.
     *
     * If a path is given (e.g., /v1), the RPC endpoint will be `/v1/(class name)/(method name)`
     */
    String path() default "";
    String description() default "";
}
