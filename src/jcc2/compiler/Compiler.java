
package jcc2.compiler;

/**
 *
 * @author note173@gmail.com
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;
import jcc2.parser.*;
import jcc2.common.*;

import jcc2.io.fs.*;

public class Compiler
{
    public static final int VERSION = 1;

    SymLocator locator;
    CodeGenerator generator;
    ClassContainer mainClass;
    Vector vBreakStack;
    Vector vContinueStack;

    public byte[] code;

    public Compiler (ASTCompilationUnit tree, boolean bJasmin) throws JccException, IOException
    {
        locator = new SymLocator ();
        vBreakStack = new Vector ();
        vContinueStack = new Vector ();

        if (bJasmin)
            generator = new JvmCodeGenerator();
        else
            generator = new JccCodeGenerator();

        mainClass = new ClassContainer ("JccMain");
        locator.setClass(mainClass);

        compileClass (tree);

        ByteArrayOutputStream bosLink = new ByteArrayOutputStream ();
        DataOutputStream osLink = new DataOutputStream (bosLink);
        generator.link(osLink, mainClass, locator);
        //FileSystem fs = FileSystem.GetInstance();
        //FilePtr f = fs.Open("/" + fs.List("/")[0] + "test.jbin", FileSystem.WRITE);
        bosLink.close();
        byte[] buf = bosLink.toByteArray();
        code = buf;
        //f.Write(buf, 0, buf.length);
        //f.Close();
    }

    public void compileClass (Node classNode) throws JccException, IOException
    {
        int line = ((SimpleNode)classNode).jjtGetFirstToken().beginLine;

        for (int i = 0; i < classNode.jjtGetNumChildren(); i++)
        {
            Node node = classNode.jjtGetChild(i);
            if (node instanceof ASTUse)
            {
                String sLib = ((ASTSingleId)node.jjtGetChild(0)).str;
                String sMsg = locator.importLibrary(sLib);
                if (sMsg != null)
                    throw new JccException (line + ": " + sMsg);
            }
            else if (node instanceof ASTMethodDeclaration)
            {
                compileMethod (node);
            }
            else if (node instanceof ASTFieldDeclaration)
            {
                Type localType = parseType (node.jjtGetChild(0));
                Node varDeclarator = node.jjtGetChild(1);
                String localName = ((ASTSingleId)varDeclarator.jjtGetChild(0)).str;
                FieldContainer fieldContainer = new FieldContainer (localName, localType);
                if (!mainClass.addField(fieldContainer))
                    throw new JccException ("" + line + ": '" + localName + "' redefinition");
                fieldContainer.num = mainClass.nextGlobal++;

                if (varDeclarator.jjtGetNumChildren() > 1)
                {
                    generator.loadThis();
                    Node varInit = varDeclarator.jjtGetChild(1);
                    Type expType = compileExpression (null, varInit, localType);
                    if (!expType.equals(localType))
                        throw new JccException ("" + line + ": '" + localName + "' incompatible types");
                    generator.setInitMethod();
                    generator.setGlobal(fieldContainer);
                }
            }
        }
    }

    public void compileMethod (Node methodNode) throws JccException, IOException
    {
        int line = ((SimpleNode)methodNode).jjtGetFirstToken().beginLine;

        Node typeNode = methodNode.jjtGetChild(0);
        Type retType = parseType (typeNode);
        Node methodDeclarator = (ASTMethodDeclarator)methodNode.jjtGetChild(1);
        String methodName = ((ASTSingleId)methodDeclarator.jjtGetChild(0)).str;
        Node formalParameters = (ASTFormalParameters)methodDeclarator.jjtGetChild(1);
        Type[] vTypes = new Type[formalParameters.jjtGetNumChildren()];
        FieldContainer[] vFields = new FieldContainer[vTypes.length];
        for (int i = 0; i < vTypes.length; i++)
        {
            Node formalParameter = formalParameters.jjtGetChild(i);
            vTypes[i] = parseType (formalParameter.jjtGetChild(0));
            String paramName = ((ASTSingleId)formalParameter.jjtGetChild(1)).str;
            vFields[i] = new FieldContainer (paramName, vTypes[i]);
        }
        MethodContainer methodContainer = new MethodContainer(methodName, retType, vTypes);
        locator.enterBlock();
        for (int i = 0; i < vFields.length; i++)
        {
            methodContainer.addLocal(vFields[i]);
            locator.addField(vFields[i]);
        }
        generator.setCurrentMethod(methodContainer);
        locator.addMethod (methodContainer);
        compileCode (methodContainer, methodNode.jjtGetChild(2), true);
        if (methodContainer.ret.equals(Type.TYPE_VOID))
        {
            generator.retVoid ();
        }
        generator.nop();
        locator.leaveBlock();
    }

    public void compileCode (MethodContainer method, Node codeNode, boolean supressBlock) throws JccException, IOException
    {
        int line = ((SimpleNode)codeNode).jjtGetFirstToken().beginLine;

        if (codeNode instanceof ASTBlock)
        {
            if (!supressBlock)
                locator.enterBlock();
            for (int i = 0; i < codeNode.jjtGetNumChildren(); i++)
                compileCode (method, codeNode.jjtGetChild(i), false);
            if (!supressBlock)
                locator.leaveBlock();
        }
        else if (codeNode instanceof ASTStatementExpressionList)
        {
            for (int i = 0; i < codeNode.jjtGetNumChildren(); i++)
            {
                compileCode (method, codeNode.jjtGetChild(i), false);
            }
        }
        else if (codeNode instanceof ASTFieldDeclaration)
        {
            Type localType = parseType (codeNode.jjtGetChild(0));
            Node varDeclarator = codeNode.jjtGetChild(1);
            String localName = ((ASTSingleId)varDeclarator.jjtGetChild(0)).str;
            FieldContainer fieldContainer = new FieldContainer (localName, localType);
            if (!locator.addField(fieldContainer))
                throw new JccException ("" + line + ": '" + localName + "' redefinition");
            method.addLocal(fieldContainer);
            
            if (varDeclarator.jjtGetNumChildren() > 1)
            {
                Node varInit = varDeclarator.jjtGetChild(1);
                Type expType = compileExpression (method, varInit, localType);
                if (!expType.equals(localType))
                    throw new JccException ("" + line + ": '" + localName + "' incompatible types");
                generator.setLocal(method, fieldContainer);
            }
        }
        else if (codeNode instanceof ASTStatementExpression)
        {
            Type type = compileExpression (method, codeNode.jjtGetChild(0), null);
            if (!type.equals(Type.TYPE_VOID))
            {
                if (type.equals(Type.TYPE_DOUBLE) || type.equals(Type.TYPE_LONG))
                    generator.pop2 ();
                else
                    generator.pop ();
            }
        }
        else if (codeNode instanceof ASTReturnStatement)
        {
            if (codeNode.jjtGetNumChildren() == 0)
            {
                if (!method.ret.equals(Type.TYPE_VOID))
                    throw new JccException ("" + line + ": invalid return type");
                generator.retVoid();
            }
            Type type = compileExpression (method, codeNode.jjtGetChild(0), method.ret);
            if (!type.equals(method.ret))
                throw new JccException ("" + line + ": invalid return type");
            generator.ret (type);
        }
        else if (codeNode instanceof ASTIfStatement)
        {
            Object labElse = generator.reserveLabel();
            Object labEnd = generator.reserveLabel();
            Type typeCond = compileExpression (method, codeNode.jjtGetChild(0), null);
            if (!typeCond.equals(Type.TYPE_BOOL))
                throw new JccException ("" + line + ": condition must be bool");
            generator.ifeq(labElse);
            compileCode (method, codeNode.jjtGetChild(1), false);
            generator.jmp(labEnd);
            generator.markLabel(labElse);
            if (codeNode.jjtGetNumChildren() > 2)
                compileCode (method, codeNode.jjtGetChild(2), false);
            generator.markLabel(labEnd);
        }
        else if (codeNode instanceof ASTWhileStatement)
        {
            Object labBegin = generator.reserveLabel();
            Object labEnd = generator.reserveLabel();
            vBreakStack.addElement(labEnd);
            vContinueStack.addElement (labBegin);
            generator.markLabel(labBegin);
            Type typeCond = compileExpression (method, codeNode.jjtGetChild(0), null);
            if (!typeCond.equals(Type.TYPE_BOOL))
                throw new JccException ("" + line + ": condition must be bool");
            generator.ifeq(labEnd);
            compileCode (method, codeNode.jjtGetChild(1), false);
            generator.jmp(labBegin);
            generator.markLabel(labEnd);
            vBreakStack.removeElementAt(vBreakStack.size()-1);
            vContinueStack.removeElementAt(vContinueStack.size()-1);
        }
        else if (codeNode instanceof ASTDoStatement)
        {
            Object labBegin = generator.reserveLabel();
            Object labEnd = generator.reserveLabel();
            vBreakStack.addElement(labEnd);
            vContinueStack.addElement(labBegin);
            generator.markLabel(labBegin);
            compileCode (method, codeNode.jjtGetChild(0), false);
            Type typeCond = compileExpression (method, codeNode.jjtGetChild(1), null);
            if (!typeCond.equals(Type.TYPE_BOOL))
                throw new JccException ("" + line + ": condition must be bool");
            generator.ifeq(labEnd);
            generator.jmp(labBegin);
            generator.markLabel(labEnd);
            vBreakStack.removeElementAt(vBreakStack.size()-1);
        }
        else if (codeNode instanceof ASTForStatement)
        {
            locator.enterBlock();
            Object labBegin = generator.reserveLabel();
            Object labEnd = generator.reserveLabel();
            Object labUpdate = generator.reserveLabel();
            vContinueStack.addElement(labUpdate);
            vBreakStack.addElement(labEnd);
            int mask = ((ASTForStatement)codeNode).num;
            int id = 0;
            if ((mask & 1)!=0) // for initializer exists
            {
                compileCode (method, codeNode.jjtGetChild(id), true);
                id++;
            }
            generator.markLabel(labBegin);
            if ((mask & 2)!=0) // condition exists
            {
                Type typeCond = compileExpression (method, codeNode.jjtGetChild(id), null);
                if (!typeCond.equals(Type.TYPE_BOOL))
                    throw new JccException ("" + line + ": condition must be bool");
                id++;
                generator.ifeq(labEnd);
            }
            int idUpdater = -1;
            if ((mask & 4)!=0) //for updater exists
            {
                idUpdater = id;
                id++;
            }
            compileCode (method, codeNode.jjtGetChild(id), false);
            generator.markLabel(labUpdate);
            if (idUpdater != -1)
            {
                compileCode (method, codeNode.jjtGetChild(idUpdater), false);
            }
            generator.jmp(labBegin);
            generator.markLabel(labEnd);
            locator.leaveBlock();
            vBreakStack.removeElementAt(vBreakStack.size()-1);
            vContinueStack.removeElementAt(vContinueStack.size()-1);
        }
        else if (codeNode instanceof ASTBreakStatement)
        {
            if (vBreakStack.size() == 0)
                throw new JccException (line + ": " + "'break' without a loop");
            generator.jmp(vBreakStack.elementAt(vBreakStack.size()-1));
        }
        else if (codeNode instanceof ASTContinueStatement)
        {
            if (vContinueStack.size() == 0)
                throw new JccException (line + ": " + "'continue' without a loop");
            generator.jmp(vContinueStack.elementAt(vContinueStack.size()-1));
        }
        else
            throw new JccException (line + ": not a statement");
    }

    Type compileExpression (MethodContainer method, Node expNode, Type suggestType) throws JccException, IOException
    {
        int line = ((SimpleNode)expNode).jjtGetFirstToken().beginLine;

        if (expNode instanceof ASTArrayInitializer)
        {
            int nDims = expNode.jjtGetNumChildren();

            Type subType = null;
            if (suggestType != null)
                subType = suggestType.arrayType();
            generator.pushInt(nDims);
            generator.newArray(suggestType.arrayType());
            for (int i = 0; i < nDims; i++)
            {
                generator.dup ();
                generator.pushInt(i);
                Type elemType = compileExpression(method, expNode.jjtGetChild(i), subType);
                if (suggestType != null && !elemType.equals(subType))
                    throw new JccException ("" + line + ": incompatible types");
                if (elemType.equals(Type.TYPE_INT))
                    generator.iastore();
                else if (elemType.equals(Type.TYPE_SHORT))
                    generator.sastore();
                else if (elemType.equals(Type.TYPE_CHAR))
                    generator.castore();
                else if (elemType.equals(Type.TYPE_BYTE) || elemType.equals(Type.TYPE_BOOL))
                    generator.bastore();
                else if (elemType.equals(Type.TYPE_LONG))
                    generator.lastore();
                else if (elemType.equals(Type.TYPE_FLOAT))
                    generator.fastore();
                else if (elemType.equals(Type.TYPE_DOUBLE))
                    generator.dastore();
                else if (elemType.equals(Type.TYPE_STRING) || elemType.kind == Type.K_OBJECT)
                    generator.aastore();
                else
                    throw new JccException ("" + line + ": incompatible types (or I was wrong)");
            }

            return suggestType;
        }
        else if (expNode instanceof ASTAssignNode)
        {
            Node nodeSign = expNode.jjtGetChild(1);
            if (nodeSign instanceof ASTMulAssign || nodeSign instanceof ASTDivAssign ||
                    nodeSign instanceof ASTRemAssign || nodeSign instanceof ASTPlusAssign ||
                    nodeSign instanceof ASTMinusAssign || nodeSign instanceof ASTLShiftAssign ||
                    nodeSign instanceof ASTRSignedShiftAssign || nodeSign instanceof ASTRUnsignedShiftAssign)
            {
                ASTAssignNode newNode = new ASTAssignNode(0);
                newNode.jjtSetFirstToken(((SimpleNode)expNode).jjtGetFirstToken());
                newNode.jjtAddChild(expNode.jjtGetChild(0), 0);
                newNode.jjtAddChild(new ASTAssign(0), 1);
                SimpleNode newExpr = null;

                if (nodeSign instanceof ASTMulAssign)
                {
                    newExpr = new ASTMultiplicativeNode (0);
                    newExpr.jjtAddChild(new ASTMul(0), 1);
                }
                else if (nodeSign instanceof ASTDivAssign)
                {
                    newExpr = new ASTMultiplicativeNode (0);
                    newExpr.jjtAddChild(new ASTDiv(0), 1);
                }
                else if (nodeSign instanceof ASTRemAssign)
                {
                    newExpr = new ASTMultiplicativeNode (0);
                    newExpr.jjtAddChild(new ASTRem(0), 1);
                }
                else if (nodeSign instanceof ASTPlusAssign)
                {
                    newExpr = new ASTAdditiveNode (0);
                    newExpr.jjtAddChild(new ASTPlus(0), 1);
                }
                else if (nodeSign instanceof ASTMinusAssign)
                {
                    newExpr = new ASTAdditiveNode (0);
                    newExpr.jjtAddChild(new ASTMinus(0), 1);
                }
                else if (nodeSign instanceof ASTLShiftAssign)
                {
                    newExpr = new ASTShiftNode (0);
                    newExpr.jjtAddChild(new ASTLShift(0), 1);
                }
                else if (nodeSign instanceof ASTRSignedShiftAssign)
                {
                    newExpr = new ASTShiftNode (0);
                    newExpr.jjtAddChild(new ASTRShift(0), 1);
                }
                else if (nodeSign instanceof ASTRUnsignedShiftAssign)
                {
                    newExpr = new ASTShiftNode (0);
                    newExpr.jjtAddChild(new ASTLShift(0), 1);
                }

                newExpr.jjtSetFirstToken(((SimpleNode)expNode.jjtGetChild(0)).jjtGetFirstToken());
                newExpr.jjtAddChild(expNode.jjtGetChild(0), 0);
                newExpr.jjtAddChild(expNode.jjtGetChild(2), 2);
                newNode.jjtAddChild(newExpr, 2);
                return compileExpression (method, newNode, null);
            }
            else if (nodeSign instanceof ASTAndAssign)
            {
                ASTAssignNode newNode = new ASTAssignNode(0);
                newNode.jjtSetFirstToken(((SimpleNode)expNode).jjtGetFirstToken());
                newNode.jjtAddChild(expNode.jjtGetChild(0), 0);
                newNode.jjtAddChild(new ASTAssign(0), 1);
                SimpleNode newExpr = new ASTAndNode(0);
                newExpr.jjtSetFirstToken(((SimpleNode)expNode.jjtGetChild(0)).jjtGetFirstToken());
                newExpr.jjtAddChild(expNode.jjtGetChild(0), 0);
                newExpr.jjtAddChild(expNode.jjtGetChild(2), 1);
                return compileExpression (method, newNode, null);
            }
            else if (nodeSign instanceof ASTOrAssign)
            {
                ASTAssignNode newNode = new ASTAssignNode(0);
                newNode.jjtSetFirstToken(((SimpleNode)expNode).jjtGetFirstToken());
                newNode.jjtAddChild(expNode.jjtGetChild(0), 0);
                newNode.jjtAddChild(new ASTAssign(0), 1);
                SimpleNode newExpr = new ASTInclusiveOrNode(0);
                newExpr.jjtSetFirstToken(((SimpleNode)expNode.jjtGetChild(0)).jjtGetFirstToken());
                newExpr.jjtAddChild(expNode.jjtGetChild(0), 0);
                newExpr.jjtAddChild(expNode.jjtGetChild(2), 1);
                return compileExpression (method, newNode, null);
            }
            else if (nodeSign instanceof ASTXorAssign)
            {
                ASTAssignNode newNode = new ASTAssignNode(0);
                newNode.jjtSetFirstToken(((SimpleNode)expNode).jjtGetFirstToken());
                newNode.jjtAddChild(expNode.jjtGetChild(0), 0);
                newNode.jjtAddChild(new ASTAssign(0), 1);
                SimpleNode newExpr = new ASTExclusiveOrNode(0);
                newExpr.jjtSetFirstToken(((SimpleNode)expNode.jjtGetChild(0)).jjtGetFirstToken());
                newExpr.jjtAddChild(expNode.jjtGetChild(0), 0);
                newExpr.jjtAddChild(expNode.jjtGetChild(2), 1);
                return compileExpression (method, newNode, null);
            }
 
            Node lvalue = expNode.jjtGetChild(0);
            if (lvalue instanceof ASTSingleId)
            {
                String varName = ((ASTSingleId)lvalue).str;
                FieldContainer field = locator.getField(varName);
                boolean bGlobal = false;
                if (field == null)
                {
                    field = mainClass.getField(varName);
                    if (field == null)
                        throw new JccException ("" + line + ": '" + varName + "' undeclarated");
                    generator.loadThis();
                    bGlobal = true;
                }
                if (nodeSign instanceof ASTAssign)
                {
                    Type type = compileExpression (method, expNode.jjtGetChild(2), null);
                    if (type.equals(Type.TYPE_DOUBLE) || type.equals(Type.TYPE_LONG))
                        if (bGlobal)
                            generator.dup2_x1();
                        else
                            generator.dup2();
                    else
                        if (bGlobal)
                            generator.dup_x1();
                        else
                            generator.dup();
                    if (!type.equals(field.type))
                        throw new JccException ("" + line + ": incompatible types");
                    if (bGlobal)
                        generator.setGlobal(field);
                    else
                        generator.setLocal(method, field);
                    return field.type;
                }
                else
                    throw new JccException ("" + line + ": not supported yet");
            }
            else if (lvalue instanceof ASTElementAccessNode)
            {
                Type type = compileExpression (method, lvalue.jjtGetChild(0), null);
                Type arrayType = type.arrayType();
                if (arrayType == null)
                    throw new JccException ("" + line + ": incompatible types");
                Type indexType = compileExpression (method, lvalue.jjtGetChild(1), null);
                if (!indexType.equals(Type.TYPE_INT))
                    throw new JccException ("" + line + ": array index: int required");
                if (nodeSign instanceof ASTAssign)
                {
                    Type valType = compileExpression (method, expNode.jjtGetChild(2), null);
                    if (!valType.equals(arrayType))
                        throw new JccException ("" + line + ": incompatible types");
                    if (valType.equals(Type.TYPE_LONG) || valType.equals(Type.TYPE_DOUBLE))
                        generator.dup2_x2();
                    else
                        generator.dup_x2();
                    if (valType.equals(Type.TYPE_BOOL) || valType.equals(Type.TYPE_BYTE))
                        generator.bastore();
                    else if (valType.equals(Type.TYPE_CHAR))
                        generator.castore();
                    else if (valType.equals(Type.TYPE_DOUBLE))
                        generator.dastore();
                    else if (valType.equals(Type.TYPE_FLOAT))
                        generator.fastore();
                    else if (valType.equals(Type.TYPE_INT))
                        generator.iastore();
                    else if (valType.equals(Type.TYPE_LONG))
                        generator.lastore();
                    else if (valType.equals(Type.TYPE_SHORT))
                        generator.sastore();
                    else
                        generator.aastore();
                    return valType;
                }
                else
                    throw new JccException ("" + line + ": not supported yet");
            }
            else
                throw new JccException ("" + line + ": not supported yet");
        }
        else if (expNode instanceof ASTConditionalExpressionNode)
        {
            Type condType = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            if (!condType.equals (Type.TYPE_BOOL))
                throw new JccException ("" + line + ": condition must be bool");
            Object labNe = generator.reserveLabel();
            Object labEnd = generator.reserveLabel();
            generator.ifeq(labNe); //if zero
            Type tEq = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            generator.jmp(labEnd);
            generator.markLabel(labNe);
            Type tNe = compileExpression (method, expNode.jjtGetChild(2), suggestType);
            generator.markLabel(labEnd);
            if (!tNe.equals(tEq))
                throw new JccException ("" + line + ": incompatible types");
            return tEq;
        }
        else if (expNode instanceof ASTOrNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            if (!type1.equals(Type.TYPE_BOOL) || !type2.equals(Type.TYPE_BOOL))
                throw new JccException ("" + line + ": '||': both operands must be bool");
            generator.ior();
            return Type.TYPE_BOOL;
        }
        else if (expNode instanceof ASTAndNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            if (!type1.equals(Type.TYPE_BOOL) || !type2.equals(Type.TYPE_BOOL))
                throw new JccException ("" + line + ": '||': both operands must be bool");
            generator.iand();
            return Type.TYPE_BOOL;
        }
        else if (expNode instanceof ASTInclusiveOrNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            if (!(type1.equals(type2) && (type1.equals(Type.TYPE_BYTE) ||
                                          type1.equals(Type.TYPE_CHAR) ||
                                          type1.equals(Type.TYPE_INT) ||
                                          type1.equals(Type.TYPE_LONG) ||
                                          type1.equals(Type.TYPE_SHORT)
                                          )))
            {
                throw new JccException ("" + line + ": '|': incompatible types");
            }
            if (type1.equals(Type.TYPE_LONG))
                generator.lor();
            else
                generator.ior();
            return type1;
        }
        else if (expNode instanceof ASTExclusiveOrNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            if (!(type1.equals(type2) && (type1.equals(Type.TYPE_BYTE) ||
                                          type1.equals(Type.TYPE_CHAR) ||
                                          type1.equals(Type.TYPE_INT) ||
                                          type1.equals(Type.TYPE_LONG) ||
                                          type1.equals(Type.TYPE_SHORT)
                                          )))
            {
                throw new JccException ("" + line + ": '^': incompatible types");
            }
            if (type1.equals(Type.TYPE_LONG))
                generator.lxor();
            else
                generator.ixor();
            return type1;
        }
        else if (expNode instanceof ASTBitAndNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            if (!(type1.equals(type2) && (type1.equals(Type.TYPE_BYTE) ||
                                          type1.equals(Type.TYPE_CHAR) ||
                                          type1.equals(Type.TYPE_INT) ||
                                          type1.equals(Type.TYPE_LONG) ||
                                          type1.equals(Type.TYPE_SHORT)
                                          )))
            {
                throw new JccException ("" + line + ": '|': incompatible types");
            }
            if (type1.equals(Type.TYPE_LONG))
                generator.land();
            else
                generator.iand();
            return type1;
        }
        else if (expNode instanceof ASTEqualityNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(2), suggestType);
            if (!type1.equals(type2) &&
                    !((type1.kind == Type.K_OBJECT && type2.equals(Type.TYPE_NULL)) ||
                    (type2.kind == Type.K_OBJECT && type1.equals(Type.TYPE_NULL))))
                throw new JccException ("" + line + ": equality: incompatible types");

            if( type1.equals(Type.TYPE_BYTE) ||
                type1.equals(Type.TYPE_CHAR) ||
                type1.equals(Type.TYPE_INT) ||
                type1.equals(Type.TYPE_SHORT) ||
                type1.equals(Type.TYPE_BOOL))
            {
                generator.ieq ();
            }
            else if (type1.equals(Type.TYPE_LONG))
            {
                generator.leq();
            }
            else if (type1.equals(Type.TYPE_FLOAT))
            {
                generator.feq ();
            }
            else if (type1.equals(Type.TYPE_DOUBLE))
            {
                generator.deq();
            }
            else if (type1.kind == Type.K_OBJECT || type1.equals(Type.TYPE_NULL))
            {
                generator.aeq();
            }
            else
            {
                throw new JccException ("" + line + ": equality: unsupported types");
            }

            if (expNode.jjtGetChild(1) instanceof ASTNotEquals)
            {
                generator.pushInt(1);
                generator.isub ();
            }

            return Type.TYPE_BOOL;
        }
        else if (expNode instanceof ASTInstanceOfNode)
        {
            Type type = compileExpression (method, expNode.jjtGetChild(0), null);
            if (type.kind != Type.K_OBJECT)
                throw new JccException ("" + line + ": 'instanceof': object required");
            Type instanceType = parseType (expNode.jjtGetChild(1));
            generator.instance_of (instanceType);
            return Type.TYPE_BOOL;
        }
        else if (expNode instanceof ASTEqualsNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), null);
            Type type2 = compileExpression (method, expNode.jjtGetChild(1), null);
            if (type1.kind != Type.K_OBJECT || type2.kind != Type.K_OBJECT)
                throw new JccException ("" + line + ": 'equals': objects required");
            generator.objectEquals();
            return Type.TYPE_BOOL;
        }
        else if (expNode instanceof ASTRelationalNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(2), suggestType);
            if (!type1.equals(type2))
                throw new JccException ("" + line + ": '==': incompatible types");

            Node nodeSign = expNode.jjtGetChild(1);

            if( type1.equals(Type.TYPE_BYTE) ||
                type1.equals(Type.TYPE_CHAR) ||
                type1.equals(Type.TYPE_INT) ||
                type1.equals(Type.TYPE_SHORT))
            {
                if (nodeSign instanceof ASTLess)
                    generator.iless ();
                else if (nodeSign instanceof ASTGreater)
                    generator.igreater ();
                else if (nodeSign instanceof ASTLessEqual)
                    generator.ileq ();
                else if (nodeSign instanceof ASTGreaterEqual)
                    generator.igeq ();
            }
            else if (type1.equals(Type.TYPE_LONG))
            {
                if (nodeSign instanceof ASTLess)
                    generator.lless ();
                else if (nodeSign instanceof ASTGreater)
                    generator.lgreater ();
                else if (nodeSign instanceof ASTLessEqual)
                    generator.lleq ();
                else if (nodeSign instanceof ASTGreaterEqual)
                    generator.lgeq ();
            }
            else if (type1.equals(Type.TYPE_FLOAT))
            {
                if (nodeSign instanceof ASTLess)
                    generator.fless ();
                else if (nodeSign instanceof ASTGreater)
                    generator.fgreater ();
                else if (nodeSign instanceof ASTLessEqual)
                    generator.fleq ();
                else if (nodeSign instanceof ASTGreaterEqual)
                    generator.fgeq ();
            }
            else if (type1.equals(Type.TYPE_DOUBLE))
            {
                if (nodeSign instanceof ASTLess)
                    generator.dless ();
                else if (nodeSign instanceof ASTGreater)
                    generator.dgreater ();
                else if (nodeSign instanceof ASTLessEqual)
                    generator.dleq ();
                else if (nodeSign instanceof ASTGreaterEqual)
                    generator.dgeq ();
            }
            else
            {
                throw new JccException ("" + line + ": relational: unsupported types");
            }

            return Type.TYPE_BOOL;
        }
        else if (expNode instanceof ASTShiftNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(2), suggestType);
            if (!type2.equals(Type.TYPE_INT))
                throw new JccException ("" + line + ": shift: int required");

            Node nodeSign = expNode.jjtGetChild(1);

            if( type1.equals(Type.TYPE_BYTE) ||
                type1.equals(Type.TYPE_CHAR) ||
                type1.equals(Type.TYPE_INT) ||
                type1.equals(Type.TYPE_SHORT))
            {
                if (nodeSign instanceof ASTLShift)
                    generator.ishl ();
                else if (nodeSign instanceof ASTRShift)
                    generator.ishr ();
                else if (nodeSign instanceof ASTRUShift)
                    generator.iushr ();
                return type1;
            }
            else if (type1.equals(Type.TYPE_LONG))
            {
                if (nodeSign instanceof ASTLShift)
                    generator.lshl ();
                else if (nodeSign instanceof ASTRShift)
                    generator.lshr ();
                else if (nodeSign instanceof ASTRUShift)
                    generator.lushr ();
                return type1;
            }
            else
            {
                throw new JccException ("" + line + ": shift: unsupported type");
            }
        }
        else if (expNode instanceof ASTAdditiveNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(2), suggestType);
            Node nodeSign = expNode.jjtGetChild(1);
            if (type1.equals(Type.TYPE_INT))
            {
                if (type2.equals(Type.TYPE_INT) || type2.equals(Type.TYPE_BYTE) ||
                        type2.equals(Type.TYPE_CHAR) || type2.equals(Type.TYPE_SHORT))
                {
                    if (nodeSign instanceof ASTPlus)
                        generator.iadd();
                    else
                        generator.isub();
                    return Type.TYPE_INT;
                }
                else
                    throw new JccException ("" + line + ": additive: int required");
            }
            else if (type1.equals(Type.TYPE_SHORT))
            {
                if (type2.equals(Type.TYPE_BYTE) ||
                        type2.equals(Type.TYPE_CHAR) || type2.equals(Type.TYPE_SHORT))
                {
                    if (nodeSign instanceof ASTPlus)
                        generator.iadd();
                    else
                        generator.isub();
                    return Type.TYPE_SHORT;
                }
                else
                    throw new JccException ("" + line + ": additive: short required");
            }
            else if (type1.equals(Type.TYPE_CHAR))
            {
                if (type2.equals(Type.TYPE_BYTE) ||
                        type2.equals(Type.TYPE_CHAR))
                {
                    if (nodeSign instanceof ASTPlus)
                        generator.iadd();
                    else
                        generator.isub();
                    return Type.TYPE_CHAR;
                }
                else
                    throw new JccException ("" + line + ": additive: char required");
            }
            else if (type1.equals(Type.TYPE_BYTE))
            {
                if (type2.equals(Type.TYPE_BYTE))
                {
                    if (nodeSign instanceof ASTPlus)
                        generator.iadd();
                    else
                        generator.isub();
                    return Type.TYPE_BYTE;
                }
                else
                    throw new JccException ("" + line + ": additive: byte required");
            }
            else if (type1.equals(Type.TYPE_LONG))
            {
                if (type2.equals(Type.TYPE_LONG))
                {
                    if (nodeSign instanceof ASTPlus)
                        generator.ladd();
                    else
                        generator.lsub();
                    return Type.TYPE_LONG;
                }
                else if (type2.equals(Type.TYPE_INT) || type2.equals(Type.TYPE_BYTE) ||
                        type2.equals(Type.TYPE_CHAR) || type2.equals(Type.TYPE_SHORT))
                {
                    generator.i2l ();
                    if (nodeSign instanceof ASTPlus)
                        generator.ladd();
                    else
                        generator.lsub();
                    return Type.TYPE_LONG;
                }
                else
                    throw new JccException ("" + line + ": additive: long or int required");
            }
            else if (type1.equals(Type.TYPE_FLOAT))
            {
                if (type2.equals(Type.TYPE_FLOAT))
                {
                    if (nodeSign instanceof ASTPlus)
                        generator.fadd();
                    else
                        generator.fsub();
                    return Type.TYPE_FLOAT;
                }
                else
                    throw new JccException ("" + line + ": additive: float required");
            }
            else if (type1.equals(Type.TYPE_DOUBLE))
            {
                if (type2.equals(Type.TYPE_DOUBLE))
                {
                    if (nodeSign instanceof ASTPlus)
                        generator.dadd();
                    else
                        generator.dsub();
                    return Type.TYPE_DOUBLE;
                }
                else if (type2.equals(Type.TYPE_FLOAT))
                {
                    generator.f2d();
                    if (nodeSign instanceof ASTPlus)
                        generator.dadd();
                    else
                        generator.dsub();
                    return Type.TYPE_DOUBLE;
                }
                else
                    throw new JccException ("" + line + ": additive: double or float required");
            }
            else if (type1.equals(Type.TYPE_STRING))
            {
                if (type2.equals(Type.TYPE_INT) || type2.equals(Type.TYPE_SHORT) ||
                        type2.equals(Type.TYPE_BYTE))
                {
                    generator.intToStr ();
                    generator.concatStr();
                }
                else if (type2.equals(Type.TYPE_LONG))
                {
                    generator.longToStr ();
                    generator.concatStr();
                }
                else if (type2.equals(Type.TYPE_FLOAT))
                {
                    generator.floatToStr ();
                    generator.concatStr();
                }
                else if (type2.equals(Type.TYPE_DOUBLE))
                {
                    generator.doubleToStr ();
                    generator.concatStr();
                }
                else if (type2.equals(Type.TYPE_STRING))
                {
                    generator.concatStr();
                }
                else
                    throw new JccException ("" + line + ": concat: invalid operand");
                return Type.TYPE_STRING;
            }
            else
                throw new JccException ("" + line + ": additive: invalid type");
        }
        else if (expNode instanceof ASTMultiplicativeNode)
        {
            Type type1 = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type type2 = compileExpression (method, expNode.jjtGetChild(2), suggestType);
            Node nodeSign = expNode.jjtGetChild(1);

            if (type1.equals(Type.TYPE_INT))
            {
                if (type2.equals(Type.TYPE_INT) ||
                        type2.equals(Type.TYPE_SHORT) ||
                        type2.equals(Type.TYPE_CHAR) ||
                        type2.equals(Type.TYPE_BYTE))
                {
                    if (nodeSign instanceof ASTMul)
                        generator.imul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.idiv();
                    else if (nodeSign instanceof ASTRem)
                        generator.irem();
                }
                else
                    throw new JccException ("" + line + ": multiplicative: int required");
                return Type.TYPE_INT;
            }
            else if (type1.equals(Type.TYPE_SHORT))
            {
                if (type2.equals(Type.TYPE_SHORT) || type2.equals(Type.TYPE_CHAR) ||
                        type2.equals(Type.TYPE_BYTE))
                {
                    if (nodeSign instanceof ASTMul)
                        generator.imul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.idiv();
                    else if (nodeSign instanceof ASTRem)
                        generator.irem();
                }
                else
                    throw new JccException ("" + line + ": multiplicative: short required");
                return Type.TYPE_SHORT;
            }
            else if (type1.equals(Type.TYPE_CHAR))
            {
                if (type2.equals(Type.TYPE_CHAR) ||
                        type2.equals(Type.TYPE_BYTE))
                {
                    if (nodeSign instanceof ASTMul)
                        generator.imul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.idiv();
                    else if (nodeSign instanceof ASTRem)
                        generator.irem();
                }
                else
                    throw new JccException ("" + line + ": multiplicative: char required");
                return Type.TYPE_CHAR;
            }
            else if (type1.equals(Type.TYPE_BYTE))
            {
                if (type2.equals(Type.TYPE_BYTE))
                {
                    if (nodeSign instanceof ASTMul)
                        generator.imul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.idiv();
                    else if (nodeSign instanceof ASTRem)
                        generator.irem();
                }
                else
                    throw new JccException ("" + line + ": multiplicative: byte required");
                return Type.TYPE_BYTE;
            }
            else if (type1.equals(Type.TYPE_LONG))
            {
                if (type2.equals(Type.TYPE_LONG))
                {
                    if (nodeSign instanceof ASTMul)
                        generator.lmul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.ldiv();
                    else if (nodeSign instanceof ASTRem)
                        generator.lrem();
                }
                else if (type2.equals(Type.TYPE_INT) ||
                        type2.equals(Type.TYPE_SHORT) ||
                        type2.equals(Type.TYPE_CHAR) ||
                        type2.equals(Type.TYPE_BYTE))
                {
                    generator.i2l();
                    if (nodeSign instanceof ASTMul)
                        generator.lmul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.ldiv();
                    else if (nodeSign instanceof ASTRem)
                        generator.lrem();
                }
                else
                    throw new JccException ("" + line + ": multiplicative: long or int required");
                return Type.TYPE_LONG;
            }
            else if (type1.equals(Type.TYPE_FLOAT))
            {
                if (type2.equals(Type.TYPE_FLOAT))
                {
                    if (nodeSign instanceof ASTMul)
                        generator.fmul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.fdiv();
                    else
                        throw new JccException ("" + line + ": '%': floating point");
                }
                else
                    throw new JccException ("" + line + ": multiplicative: float required");
                return Type.TYPE_FLOAT;
            }
            else if (type1.equals(Type.TYPE_DOUBLE))
            {
                if (type2.equals(Type.TYPE_DOUBLE))
                {
                    if (nodeSign instanceof ASTMul)
                        generator.dmul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.ddiv();
                    else
                        throw new JccException ("" + line + ": '%': floating point");
                }
                if (type2.equals(Type.TYPE_FLOAT))
                {
                    generator.f2d();
                    if (nodeSign instanceof ASTMul)
                        generator.dmul();
                    else if (nodeSign instanceof ASTDiv)
                        generator.ddiv();
                    else
                        throw new JccException ("" + line + ": '%': floating point");
                }
                else
                    throw new JccException ("" + line + ": multiplicative: double or float required");
                return Type.TYPE_DOUBLE;
            }
            else
                throw new JccException ("" + line + ": multiplicative: unsupported types");
        }
        else if (expNode instanceof ASTUnaryMinus)
        {
            Type type = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            if (type.equals(Type.TYPE_INT) || type.equals(Type.TYPE_SHORT) ||
                    type.equals(Type.TYPE_CHAR) || type.equals(Type.TYPE_BYTE))
                generator.ineg();
            else if (type.equals(Type.TYPE_LONG))
                generator.lneg();
            else if (type.equals(Type.TYPE_FLOAT))
                generator.fneg();
            else if (type.equals(Type.TYPE_DOUBLE))
                generator.dneg();
            else
                throw new JccException ("" + line + ": '-': invalid type");
            return type;
        }
        else if (expNode instanceof ASTPreIncrementExpression || expNode instanceof ASTPreDecrementExpression)
        {
            Node node = expNode.jjtGetChild(0);
            if (node instanceof ASTElementAccessNode)
            {
                Type arrayType = compileExpression (method, node.jjtGetChild(0), null);
                Type elementType = arrayType.arrayType();
                if (elementType == null)
                    throw new JccException ("" + line + ": invalid lvalue");
                Type indexType = compileExpression (method, node.jjtGetChild(1), null);
                if (!(indexType.equals(Type.TYPE_INT) || indexType.equals(Type.TYPE_SHORT) ||
                        indexType.equals(Type.TYPE_CHAR) || indexType.equals(Type.TYPE_BYTE)))
                    throw new JccException ("" + line + ": index: int required");
                
                generator.dup2();

                if (elementType.equals(Type.TYPE_INT))
                {
                    generator.iaload();

                    if (expNode instanceof ASTPreIncrementExpression)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.dup_x2();
                    generator.iastore();
                    return Type.TYPE_INT;
                }
                else if (elementType.equals(Type.TYPE_SHORT))
                {
                    generator.saload();
                    if (expNode instanceof ASTPreIncrementExpression)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.dup_x2();
                    generator.sastore();
                    return Type.TYPE_SHORT;
                }
                else if (elementType.equals(Type.TYPE_CHAR))
                {
                    generator.caload();
                    if (expNode instanceof ASTPreIncrementExpression)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.dup_x2();
                    generator.castore();
                    return Type.TYPE_CHAR;
                }
                else if (elementType.equals(Type.TYPE_BYTE))
                {
                    generator.baload();
                    if (expNode instanceof ASTPreIncrementExpression)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.dup_x2();
                    generator.bastore();
                    return Type.TYPE_BYTE;
                }
                else if (elementType.equals(Type.TYPE_LONG))
                {
                    generator.laload();
                    if (expNode instanceof ASTPreIncrementExpression)
                        generator.linc();
                    else
                    {
                        generator.pushLong (1);
                        generator.lsub ();
                    }
                    generator.dup2_x2();
                    generator.lastore();
                    return Type.TYPE_LONG;
                }
                else
                    throw new JccException ("" + line + ": inc: int required");
            }
            else if (node instanceof ASTSingleId)
            {
                String sId = ((ASTSingleId)node).str;
                FieldContainer field = locator.getField(sId);
                boolean bGlobal = false;
                if (field != null)
                    generator.getLocal(method, field);
                else
                {
                    generator.loadThis();
                    bGlobal = true;
                    field = mainClass.getField(sId);
                    if (field != null)
                        generator.getGlobal(field);
                    else
                        throw new JccException ("" + line + ": '" + sId + "' not declarated");
                }
                Type type = field.type;
                if (type.equals(Type.TYPE_BYTE) || type.equals(Type.TYPE_CHAR) ||
                        type.equals(Type.TYPE_SHORT) || type.equals(Type.TYPE_INT))
                {
                    if (expNode instanceof ASTPreIncrementExpression)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    if (bGlobal)
                        generator.dup_x1();
                    else
                        generator.dup();
                }
                else if (type.equals(Type.TYPE_LONG))
                {
                    if (expNode instanceof ASTPreIncrementExpression)
                        generator.linc();
                    else
                    {
                        generator.pushLong (1);
                        generator.lsub ();
                    }
                    if (bGlobal)
                        generator.dup2_x1();
                    else
                        generator.dup2();
                }
                else
                    throw new JccException ("" + line + ": incompatible types");
                if (bGlobal)
                    generator.setGlobal (field);
                else
                    generator.setLocal (method, field);
                return field.type;
            }
            else if (node instanceof ASTMemberAccessNode)
            {
                throw new JccException ("" + line + ": not implemented yet");
            }
            else
                throw new JccException ("" + line + ": invalid lvalue");
        }
        else if (expNode instanceof ASTNotNode)
        {
            Node node = expNode.jjtGetChild(0);
            Type type = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            if (node instanceof ASTTilda)
            {
                throw new JccException ("" + line + ": '~' not supported yet");
            }
            else if (node instanceof ASTNot)
            {
                if (type.equals(Type.TYPE_BOOL))
                {
                    generator.ineg();
                    return Type.TYPE_BOOL;
                }
                else
                    throw new JccException ("" + line + ": '!': bool required");
            }
        }
        else if (expNode instanceof ASTCastExpression)
        {
            Type castType = parseType (expNode.jjtGetChild(0));
            Type expType = compileExpression (method, expNode.jjtGetChild(1), suggestType);

            if (castType.equals(Type.TYPE_BYTE))
            {
                if (expType.equals(Type.TYPE_BYTE))
                {
                }
                else if (expType.equals(Type.TYPE_CHAR) ||
                        expType.equals(Type.TYPE_SHORT) ||
                        expType.equals(Type.TYPE_INT))
                {
                    generator.i2b ();
                }
                else if (expType.equals(Type.TYPE_LONG))
                {
                    generator.l2i();
                    generator.i2b();
                }
                else if (expType.equals(Type.TYPE_FLOAT))
                {
                    generator.f2i();
                    generator.i2b();
                }
                else if (expType.equals(Type.TYPE_DOUBLE))
                {
                    generator.d2i();
                    generator.i2b();
                }
                else
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else if (castType.equals(Type.TYPE_CHAR))
            {
                if (expType.equals(Type.TYPE_BYTE))
                {
                }
                else if (expType.equals(Type.TYPE_CHAR))
                {
                }
                else if (expType.equals(Type.TYPE_SHORT) || expType.equals(Type.TYPE_INT))
                {
                    generator.i2c();
                }
                else if (expType.equals(Type.TYPE_LONG))
                {
                    generator.l2i();
                    generator.i2c();
                }
                else if (expType.equals(Type.TYPE_FLOAT))
                {
                    generator.f2i();
                    generator.i2c();
                }
                else if (expType.equals(Type.TYPE_DOUBLE))
                {
                    generator.d2i();
                    generator.i2c();
                }
                else
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else if (castType.equals(Type.TYPE_SHORT))
            {
                if (expType.equals(Type.TYPE_BYTE) || expType.equals(Type.TYPE_CHAR) ||
                        expType.equals(Type.TYPE_SHORT))
                {
                }
                else if (expType.equals(Type.TYPE_INT))
                {
                    generator.i2s();
                }
                else if (expType.equals(Type.TYPE_LONG))
                {
                    generator.l2i();
                    generator.i2s();
                }
                else if (expType.equals(Type.TYPE_FLOAT))
                {
                    generator.f2i();
                    generator.i2s();
                }
                else if (expType.equals(Type.TYPE_DOUBLE))
                {
                    generator.d2i();
                    generator.i2s();
                }
                else
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else if (castType.equals(Type.TYPE_INT))
            {
                if (expType.equals(Type.TYPE_BYTE) || expType.equals(Type.TYPE_CHAR) ||
                        expType.equals(Type.TYPE_SHORT) || expType.equals(Type.TYPE_INT))
                {
                }
                else if (expType.equals(Type.TYPE_LONG))
                    generator.l2i();
                else if (expType.equals(Type.TYPE_FLOAT))
                    generator.f2i();
                else if (expType.equals(Type.TYPE_DOUBLE))
                    generator.d2i();
                else
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else if (castType.equals(Type.TYPE_LONG))
            {
                if (expType.equals(Type.TYPE_BYTE) || expType.equals(Type.TYPE_CHAR) ||
                        expType.equals(Type.TYPE_SHORT) || expType.equals(Type.TYPE_INT))
                {
                    generator.i2l();
                }
                else if (expType.equals(Type.TYPE_LONG))
                {
                }
                else if (expType.equals(Type.TYPE_FLOAT))
                    generator.f2l();
                else if (expType.equals(Type.TYPE_DOUBLE))
                    generator.d2l();
                else
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else if (castType.equals(Type.TYPE_FLOAT))
            {
                if (expType.equals(Type.TYPE_BYTE) || expType.equals(Type.TYPE_CHAR) ||
                        expType.equals(Type.TYPE_SHORT) || expType.equals(Type.TYPE_INT))
                    generator.i2f();
                else if (expType.equals(Type.TYPE_LONG))
                    generator.l2f();
                else if (expType.equals(Type.TYPE_FLOAT))
                {
                }
                else if (expType.equals(Type.TYPE_DOUBLE))
                    generator.d2f();
                else
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else if (castType.equals(Type.TYPE_DOUBLE))
            {
                if (expType.equals(Type.TYPE_BYTE) || expType.equals(Type.TYPE_CHAR) ||
                        expType.equals(Type.TYPE_SHORT) || expType.equals(Type.TYPE_INT))
                    generator.i2d();
                else if (expType.equals(Type.TYPE_LONG))
                    generator.l2d();
                else if (expType.equals(Type.TYPE_FLOAT))
                    generator.f2d();
                else if (expType.equals(Type.TYPE_DOUBLE))
                {
                }
                else
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else if (castType.equals(Type.TYPE_STRING))
            {
                if (expType.equals(Type.TYPE_ABYTE))
                {
                    generator.strFromByteArray();
                    return Type.TYPE_STRING;
                }
            }
            else if (castType.kind == Type.K_OBJECT)
            {
                if (expType.kind != Type.K_OBJECT)
                    throw new JccException ("" + line + ": invalid type casting");
                return castType;
            }
            else
                throw new JccException ("" + line + ": invalid type casting");
        }
        else if (expNode instanceof ASTPostfixNode)
        {
            Node node = expNode.jjtGetChild(0);
            Node operNode = expNode.jjtGetChild(1);
            if (node instanceof ASTElementAccessNode)
            {
                Type arrayType = compileExpression (method, node.jjtGetChild(0), null);
                Type elementType = arrayType.arrayType();
                if (elementType == null)
                    throw new JccException ("" + line + ": invalid lvalue");
                Type indexType = compileExpression (method, node.jjtGetChild(1), null);
                if (!(indexType.equals(Type.TYPE_INT) || indexType.equals(Type.TYPE_SHORT) ||
                        indexType.equals(Type.TYPE_CHAR) || indexType.equals(Type.TYPE_BYTE)))
                    throw new JccException ("" + line + ": index: int required");

                generator.dup2();
                if (elementType.equals(Type.TYPE_INT))
                {
                    generator.iaload();
                    generator.dup_x2();

                    if (operNode instanceof ASTPlusPlus)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.iastore();
                    return Type.TYPE_INT;
                }
                else if (elementType.equals(Type.TYPE_SHORT))
                {
                    generator.saload();
                    generator.dup_x2();
                    if (operNode instanceof ASTPlusPlus)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.sastore();
                    return Type.TYPE_SHORT;
                }
                else if (elementType.equals(Type.TYPE_CHAR))
                {
                    generator.caload();
                    generator.dup_x2();
                    if (operNode instanceof ASTPlusPlus)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.castore();
                    return Type.TYPE_CHAR;
                }
                else if (elementType.equals(Type.TYPE_BYTE))
                {
                    generator.baload();
                    generator.dup_x2();
                    if (operNode instanceof ASTPlusPlus)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                    generator.bastore();
                    return Type.TYPE_BYTE;
                }
                else if (elementType.equals(Type.TYPE_LONG))
                {
                    generator.laload();
                    generator.dup2_x2();
                    if (operNode instanceof ASTPlusPlus)
                        generator.linc();
                    else
                    {
                        generator.pushLong (1);
                        generator.lsub ();
                    }
                    generator.lastore();
                    return Type.TYPE_LONG;
                }
                else
                    throw new JccException ("" + line + ": inc: int required");
            }
            else if (node instanceof ASTSingleId)
            {
                String sId = ((ASTSingleId)node).str;
                FieldContainer field = locator.getField(sId);
                boolean bGlobal = false;
                if (field != null)
                    generator.getLocal(method, field);
                else
                {
                    generator.loadThis();
                    bGlobal = true;
                    field = mainClass.getField(sId);
                    if (field != null)
                        generator.getGlobal(field);
                    else
                        throw new JccException ("" + line + ": '" + sId + "' not declarated");
                }
                Type type = field.type;
                if (type.equals(Type.TYPE_BYTE) || type.equals(Type.TYPE_CHAR) ||
                        type.equals(Type.TYPE_SHORT) || type.equals(Type.TYPE_INT))
                {
                    if (bGlobal)
                        generator.dup_x1();
                    else
                        generator.dup();
                    if (operNode instanceof ASTPlusPlus)
                        generator.iinc();
                    else
                    {
                        generator.pushInt (1);
                        generator.isub ();
                    }
                }
                else if (type.equals(Type.TYPE_LONG))
                {
                    if (bGlobal)
                        generator.dup2_x1();
                    else
                        generator.dup2();
                    if (operNode instanceof ASTPlusPlus)
                        generator.linc();
                    else
                    {
                        generator.pushLong (1);
                        generator.lsub ();
                    }
                }
                else
                    throw new JccException ("" + line + ": incompatible types");
                if (bGlobal)
                    generator.setGlobal (field);
                else
                    generator.setLocal (method, field);
                return field.type;
            }
            else if (expNode instanceof ASTMemberAccessNode)
            {
                throw new JccException ("" + line + ": not implemented yet");
            }
            else
                throw new JccException ("" + line + ": invalid lvalue");
        }
        else if (expNode instanceof ASTElementAccessNode)
        {
            Type type = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Type indexType = compileExpression (method, expNode.jjtGetChild(1), suggestType);
            Type arrayType = type.arrayType();
            if (arrayType == null)
                throw new JccException ("" + line + ": array required");
            if (!indexType.equals(Type.TYPE_INT))
                throw new JccException ("" + line + ": index: int required");
            if (arrayType.equals(Type.TYPE_BOOL) || arrayType.equals(Type.TYPE_BYTE))
            {
                generator.baload();
                return arrayType;
            }
            else if (arrayType.equals(Type.TYPE_CHAR))
            {
                generator.caload();
                return arrayType;
            }
            else if (arrayType.equals(Type.TYPE_SHORT))
            {
                generator.saload();
                return arrayType;
            }
            else if (arrayType.equals(Type.TYPE_INT))
            {
                generator.iaload();
                return arrayType;
            }
            else if (arrayType.equals(Type.TYPE_LONG))
            {
                generator.laload();
                return arrayType;
            }
            else if (arrayType.equals(Type.TYPE_FLOAT))
            {
                generator.faload();
                return arrayType;
            }
            else if (arrayType.equals(Type.TYPE_DOUBLE))
            {
                generator.daload();
                return arrayType;
            }
            else if (arrayType.kind == Type.K_OBJECT)
            {
                generator.aaload();
                return arrayType;
            }
        }
        else if (expNode instanceof ASTMemberAccessNode)
        {
            if (expNode.jjtGetChild(0) instanceof ASTSingleId && expNode.jjtGetChild(1) instanceof ASTSingleId)
            {
                String sClass = ((ASTSingleId)(expNode.jjtGetChild(0))).str;
                String sStatic = ((ASTSingleId)(expNode.jjtGetChild(1))).str;
                ClassContainer ctClass = locator.getClass(sClass);
                if (ctClass != null)
                {
                    Object[] ctStatic = ctClass.getStatic(sStatic);
                    if (ctStatic == null)
                        throw new JccException ("" + line + ": '" + sClass + "' has no member '" + sStatic + "'");
                    Type type = (Type)ctStatic[1];
                    if (type.equals(Type.TYPE_INT))
                        generator.pushInt (((Integer)ctStatic[0]).intValue());
                    else
                        throw new JccException ("" + line + ": not supported yet");
                    return type;
                }
            }

            Type classType = compileExpression (method, expNode.jjtGetChild(0), suggestType);
            Node nodeMember = expNode.jjtGetChild(1);
            if (classType.kind != Type.K_OBJECT)
                throw new JccException ("" + line + ": '.': object required");
            if (nodeMember instanceof ASTSingleId)
            {
                String sMember = ((ASTSingleId)nodeMember).str;
                Type arrayType = classType.arrayType();
                if (arrayType != null)
                {
                    if (sMember.equals("length"))
                    {
                        generator.arraylen ();
                        return Type.TYPE_INT;
                    }
                    else
                        throw new JccException ("" + line + ": '" + sMember + "' is not a member");
                }
                else
                {
                    ClassContainer object = classType.object;
                    FieldContainer field = object.getField(sMember);
                    if (field == null)
                        throw new JccException ("" + line + ": '" + sMember + "' is not a member");
                    generator.getMember(object, field);
                    return field.type;
                }
            }
            else if (nodeMember instanceof ASTCallNode)
            {
                ClassContainer object = classType.object;
                if (object == null && !classType.equals(Type.TYPE_STRING))
                    throw new JccException ("" + line + ": object required");
                Node nodeId = nodeMember.jjtGetChild(0);
                String sMethod = ((ASTSingleId)nodeId).str;
                Node arguments = nodeMember.jjtGetChild(1);
                Type[] argTypes = new Type[arguments.jjtGetNumChildren()];
                for (int i = 0; i < arguments.jjtGetNumChildren(); i++)
                {
                    argTypes[i] = compileExpression (method, arguments.jjtGetChild(i), suggestType);
                }
                String sSpec = MethodContainer.genSpec(sMethod, argTypes);
                
                if (classType.equals(Type.TYPE_STRING))
                {
                    if (sSpec.equals("getBytes()"))
                    {
                        generator.strToByteArray();
                        return Type.TYPE_ABYTE;
                    }
                    else if (sSpec.equals("substring(I)"))
                    {
                        generator.strSubStr1();
                        return Type.TYPE_STRING;
                    }
                    else if (sSpec.equals("substring(II)"))
                    {
                        generator.strSubStr2();
                        return Type.TYPE_STRING;
                    }
                    else if (sSpec.equals("charAt(I)"))
                    {
                        generator.strCharAt ();
                        return Type.TYPE_CHAR;
                    }
                    else if (sSpec.equals("indexOf(I)") || sSpec.equals("indexOf(C)"))
                    {
                        generator.strIndexOf1();
                        return Type.TYPE_INT;
                    }
                    else if (sSpec.equals("indexOf(II)") || sSpec.equals("indexOf(CI)"))
                    {
                        generator.strIndexOf2();
                        return Type.TYPE_INT;
                    }
                    else if (sSpec.equals("indexOf(Ljava/lang/String;)"))
                    {
                        generator.strIndexOf3();
                        return Type.TYPE_INT;
                    }
                    else if (sSpec.equals("indexOf(Ljava/lang/String;I)"))
                    {
                        generator.strIndexOf4();
                        return Type.TYPE_INT;
                    }
                    else if (sSpec.equals("lastIndexOf(I)") || sSpec.equals("lastIndexOf(C)"))
                    {
                        generator.strLastIndexOf1();
                        return Type.TYPE_INT;
                    }
                    else if (sSpec.equals("lastIndexOf(II)") || sSpec.equals("lastIndexOf(CI)"))
                    {
                        generator.strLastIndexOf2();
                        return Type.TYPE_INT;
                    }
                    else if (sSpec.equals("length()"))
                    {
                        generator.strLength();
                        return Type.TYPE_INT;
                    }
                    else if (sSpec.equals("replace(CC)"))
                    {
                        generator.strReplace();
                        return Type.TYPE_STRING;
                    }
                    else if (sSpec.equals("startsWith(Ljava/lang/String;)"))
                    {
                        generator.strStartsWith();
                        return Type.TYPE_BOOL;
                    }
                    else if (sSpec.equals("endsWith(Ljava/lang/String;)"))
                    {
                        generator.strEndsWith();
                        return Type.TYPE_BOOL;
                    }
                    else if (sSpec.equals("trim()"))
                    {
                        generator.strTrim();
                        return Type.TYPE_STRING;
                    }
                    else
                        throw new JccException ("" + line + ": string has no method " + sSpec);
                }

                MethodContainer memberMethod = object.getMethod(sSpec);
                if (memberMethod == null)
                    throw new JccException ("" + line + ": '" + sMethod + "' is not a member");
                generator.callMember(object, memberMethod);
                return memberMethod.ret;
            }
            else
                throw new JccException ("" + line + ": '.': invalid operation");
        }
        else if (expNode instanceof ASTCallNode)
        {
            Node nodeId = expNode.jjtGetChild(0);
            if (!(nodeId instanceof ASTSingleId))
                throw new JccException ("" + line + ": call: identifier required");

            String sMethod = ((ASTSingleId)nodeId).str;

            Node arguments = expNode.jjtGetChild(1);
            Type[] argTypes = new Type[arguments.jjtGetNumChildren()];
            generator.fakeModeOn(); //we need this. really
            for (int i = 0; i < arguments.jjtGetNumChildren(); i++)
            {
                argTypes[i] = compileExpression (method, arguments.jjtGetChild(i), suggestType);
            }
            String sSpec = MethodContainer.genSpec(sMethod, argTypes);
            MethodContainer callMethod = locator.getMethod (sSpec);
            if (callMethod == null)
                throw new JccException ("" + line + ": '" + sSpec + "' not found");
            generator.fakeModeOff();
            if (callMethod.libHost == null)
            {
                generator.loadThis();
            }
            else
            {
                generator.loadLibraryInstance(callMethod.libHost);
            }

            for (int i = 0; i < arguments.jjtGetNumChildren(); i++)
            {
                compileExpression (method, arguments.jjtGetChild(i), suggestType);
            }
            
            generator.call(callMethod);
            return callMethod.ret;
        }
        else if (expNode instanceof ASTIntLiteral)
        {
            String sInt = ((SimpleNode)expNode).str;
            if (sInt.endsWith("l") || sInt.endsWith("L"))
            {
                long lconst;
                try {
                if (sInt.startsWith("0x"))
                {
                    String sHex = sInt.substring(2);
                    lconst = Long.parseLong (sHex, 16);
                }
                else
                {
                    lconst = Integer.parseInt(sInt);
                }
                } catch (Exception e) {
                    throw new JccException ("" + line + ": invalid integer constant '" + sInt + "'");
                }
                generator.pushLong(lconst);
                return Type.TYPE_LONG;
            }
            else
            {
                int iconst;
                try {
                if (sInt.startsWith("0x"))
                {
                    String sHex = sInt.substring(2);
                    iconst = Integer.parseInt (sHex, 16);
                }
                else
                {
                    iconst = Integer.parseInt(sInt);
                }
                } catch (Exception e) {
                    throw new JccException ("" + line + ": invalid integer constant '" + sInt + "'");
                }
                generator.pushInt(iconst);
                return Type.TYPE_INT;
            }
        }
        else if (expNode instanceof ASTFloatLiteral)
        {
            String sFloat = ((SimpleNode)expNode).str;
            if (sFloat.endsWith("f"))
            {
                float fconst;
                try {
                    fconst = Float.parseFloat(sFloat);
                } catch (Exception e) {
                    throw new JccException ("" + line + ": invalid float constant '" + sFloat + "'");
                }
                generator.pushFloat(fconst);
                return Type.TYPE_FLOAT;
            }
            else
            {
                double dconst;
                try {
                    dconst = Double.parseDouble(sFloat);
                } catch (Exception e) {
                    throw new JccException ("" + line + ": invalid double constant '" + sFloat + "'");
                }
                generator.pushDouble(dconst);
                return Type.TYPE_DOUBLE;
            }
        }
        else if (expNode instanceof ASTCharLiteral)
        {
            String sVal = ((ASTCharLiteral)expNode).str;
            char val = parseChar (sVal);
            if (val < 0)
                throw new JccException ("" + line + ": invalid char constant '" + sVal + "'");
            generator.pushInt(val);
            return Type.TYPE_CHAR;
        }
        else if (expNode instanceof ASTStringLiteral)
        {
            String sVal = ((ASTStringLiteral)expNode).str;
            String sProcessed = parseString (sVal);
            if (sProcessed == null)
                throw new JccException ("" + line + ": invalid string constant '" + sVal + "'");
            generator.pushString(sProcessed);
            return Type.TYPE_STRING;
        }
        else if (expNode instanceof ASTBooleanLiteral)
        {
            if (expNode.jjtGetChild(0) instanceof ASTTrue)
                generator.pushInt(1);
            else
                generator.pushInt(0);
            return Type.TYPE_BOOL;
        }
        else if (expNode instanceof ASTNull)
        {
            generator.pushNull ();
            return Type.TYPE_NULL;
        }
        else if (expNode instanceof ASTSingleId)
        {
            String sId = ((ASTSingleId)expNode).str;
            FieldContainer field = locator.getField(sId);
            if (field != null)
            {
                generator.getLocal(method, field);
                return field.type;
            }
            field = mainClass.getField(sId);
            if (field != null)
            {
                generator.getGlobal(field);
                return field.type;
            }
            else
                throw new JccException ("" + line + ": '" + sId + "' not declarated");
        }
        else if (expNode instanceof ASTNewArray)
        {
            Type type = parseType (expNode.jjtGetChild(0));
            Node dimsNode = expNode.jjtGetChild(1);
            int nDims = ((SimpleNode)dimsNode).num;
            if (dimsNode.jjtGetNumChildren() < 1)
                throw new JccException ("" + line + ": invalid array initializer");
            Type arrayType;
            if (type.kind == Type.K_PRIMITIVE)
                arrayType = new Type (type.primitive, nDims);
            else
            {
                if (type.primitive == Type.T_STRING)
                    arrayType = new Type (Type.T_STRING, nDims);
                else
                    arrayType = new Type (type.object, nDims);
            }
            for (int i = 0; i < dimsNode.jjtGetNumChildren(); i++)
            {
                Type countType = compileExpression (method, dimsNode.jjtGetChild(i), null);
                if (!countType.equals(Type.TYPE_INT))
                    throw new JccException ("" + line + ": array length: int required");
            }
            if (nDims == 1)
                generator.newArray(arrayType.arrayType());
            else
                generator.multianewArray (arrayType, dimsNode.jjtGetNumChildren());
            return arrayType;
        }
        else if (expNode instanceof ASTConstructor)
        {
            throw new JccException ("not implemented yet");
        }

        throw new JccException ("can't determine expression type");

        //return null;
    }

    public Type parseType (Node typeNode) throws JccException
    {
        int line = ((SimpleNode)typeNode).jjtGetFirstToken().beginLine;

        if (typeNode instanceof ASTVoidNode)
        {
            return Type.TYPE_VOID;
        }
        else
        {
            int arrayDepth = ((SimpleNode)typeNode).num;
            Node node = typeNode;
            if (node instanceof ASTType)
                node = node.jjtGetChild(0);
            if (node instanceof ASTPrimitiveType)
            {
                node = node.jjtGetChild(0);
                if (arrayDepth == 0)
                {
                    if (node instanceof ASTBoolNode)
                        return Type.TYPE_BOOL;
                    else if (node instanceof ASTCharNode)
                        return Type.TYPE_CHAR;
                    else if (node instanceof ASTByteNode)
                        return Type.TYPE_BYTE;
                    else if (node instanceof ASTShortNode)
                        return Type.TYPE_SHORT;
                    else if (node instanceof ASTIntNode)
                        return Type.TYPE_INT;
                    else if (node instanceof ASTLongNode)
                        return Type.TYPE_LONG;
                    else if (node instanceof ASTFloatNode)
                        return Type.TYPE_FLOAT;
                    else if (node instanceof ASTDoubleNode)
                        return Type.TYPE_DOUBLE;
                    else if (node instanceof ASTStringNode)
                        return Type.TYPE_STRING;
                }
                else
                {
                    if (node instanceof ASTBoolNode)
                        return new Type (Type.T_BOOL, arrayDepth);
                    else if (node instanceof ASTCharNode)
                        return new Type (Type.T_CHAR, arrayDepth);
                    else if (node instanceof ASTByteNode)
                        return new Type (Type.T_BYTE, arrayDepth);
                    else if (node instanceof ASTShortNode)
                        return new Type (Type.T_SHORT, arrayDepth);
                    else if (node instanceof ASTIntNode)
                        return new Type (Type.T_INT, arrayDepth);
                    else if (node instanceof ASTLongNode)
                        return new Type (Type.T_LONG, arrayDepth);
                    else if (node instanceof ASTFloatNode)
                        return new Type (Type.T_FLOAT, arrayDepth);
                    else if (node instanceof ASTDoubleNode)
                        return new Type (Type.T_DOUBLE, arrayDepth);
                    else if (node instanceof ASTStringNode)
                        return new Type (Type.T_STRING, arrayDepth);
                }
            }
            else
            {
                String className = "";
                for (int i = 0; i < node.jjtGetNumChildren(); i++)
                {
                    ASTSingleId singleId = (ASTSingleId)node.jjtGetChild(i);
                    className += singleId.str;
                    if (i != node.jjtGetNumChildren() - 1)
                        className += ".";
                }
                ClassContainer classContainer = locator.getClass(className);
                if (classContainer == null)
                    throw new JccException ("" + line + ": undefined class '" + className + "'");
                return new Type (classContainer, arrayDepth);
            }
        }
        return null;
    }

    char parseChar(String str) throws JccException
    {
        String s = str.substring(1, str.length()-1);
        if (s.length() < 1)
            return (char)-1;
        char c = s.charAt(0);
        if (c == '\\')
        {
            if (s.length() < 2)
                return (char)-1;
            char c2 = s.charAt(1);
            if (c2 == 'n')
                return '\n';
            else if (c2 == 'r')
                return '\r';
            else if (c2 == 't')
                return '\t';
            else
                return (char)-2;
        }
        else
        {
            return c;
        }
    }

    String parseString (String s)
    {
        String str = "";
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c == '\\')
            {
                i++;
                if (i >= s.length())
                    return null;
                char c2 = s.charAt(i);
                if (c2 == 'n')
                    str += '\n';
                else if (c2 == 'r')
                    str +='\r';
                else if (c2 == 't')
                    str += '\t';
                else
                    return null;
            }
            else
            {
                str += c;
            }
        }
        str = str.substring(1, str.length()-1);
        return str;
    }
}
