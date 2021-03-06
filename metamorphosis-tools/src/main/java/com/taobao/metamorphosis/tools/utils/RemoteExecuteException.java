/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Authors:
 *   wuhua <wq163@163.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.metamorphosis.tools.utils;

/**
 * 代表jmx相关异常
 * 
 * @author 无花
 * @since 2011-8-23 下午5:19:56
 */

public class RemoteExecuteException extends RuntimeException {

    private static final long serialVersionUID = -7410016800727397507L;


    public RemoteExecuteException(String message) {
        super(message);
    }


    public RemoteExecuteException(Throwable cause) {
        super(cause);
    }


    public RemoteExecuteException(String message, Throwable cause) {
        super(message, cause);
    }
}