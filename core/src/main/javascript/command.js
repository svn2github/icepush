/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *
 */

var register = operator();
var deserializeAndExecute = operator();

function CommandDispatcher() {
    var commands = [];

    return object(function(method) {
        method(register, function(self, messageName, command) {
            commands = reject(commands, function(cell) {
                return key(cell) == messageName;
            });
            append(commands, Cell(messageName, command));
        });

        method(deserializeAndExecute, function(self, message) {
            var messageName = message.nodeName;
            var found = detect(commands, function(cell) {
                return key(cell) == messageName;
            }, function() {
                throw 'Unknown message received: ' + messageName;
            });
            value(found)(message);
        });
    });
}

function NoopCommand() {
    debug(namespace.logger, 'received noop');
}

function ParsingError(message) {
    logger.error('Parsing error');
    var errorNode = message.firstChild;
    logger.error(errorNode.data);
    var sourceNode = errorNode.firstChild;
    logger.error(sourceNode.data);
}

function Macro(dispatcher) {
    return function(message) {
        each(message.childNodes, curry(deserializeAndExecute, dispatcher));
    };
}
