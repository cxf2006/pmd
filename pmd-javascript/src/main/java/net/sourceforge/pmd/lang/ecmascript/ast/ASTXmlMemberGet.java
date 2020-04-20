/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.ecmascript.ast;

import org.mozilla.javascript.ast.XmlMemberGet;

public final class ASTXmlMemberGet extends AbstractInfixEcmascriptNode<XmlMemberGet> {
    ASTXmlMemberGet(XmlMemberGet xmlMemberGet) {
        super(xmlMemberGet);
    }

    @Override
    public Object jjtAccept(EcmascriptParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
