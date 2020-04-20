/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.ecmascript.ast;

import org.mozilla.javascript.ast.Assignment;

public final class ASTAssignment extends AbstractInfixEcmascriptNode<Assignment> {
    ASTAssignment(Assignment asssignment) {
        super(asssignment);
    }

    @Override
    public Object jjtAccept(EcmascriptParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
