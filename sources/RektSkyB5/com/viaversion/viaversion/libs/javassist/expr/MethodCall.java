/*
 * Decompiled with CFR 0.152.
 */
package com.viaversion.viaversion.libs.javassist.expr;

import com.viaversion.viaversion.libs.javassist.CannotCompileException;
import com.viaversion.viaversion.libs.javassist.ClassPool;
import com.viaversion.viaversion.libs.javassist.CtBehavior;
import com.viaversion.viaversion.libs.javassist.CtClass;
import com.viaversion.viaversion.libs.javassist.CtMethod;
import com.viaversion.viaversion.libs.javassist.NotFoundException;
import com.viaversion.viaversion.libs.javassist.bytecode.BadBytecode;
import com.viaversion.viaversion.libs.javassist.bytecode.Bytecode;
import com.viaversion.viaversion.libs.javassist.bytecode.CodeAttribute;
import com.viaversion.viaversion.libs.javassist.bytecode.CodeIterator;
import com.viaversion.viaversion.libs.javassist.bytecode.ConstPool;
import com.viaversion.viaversion.libs.javassist.bytecode.Descriptor;
import com.viaversion.viaversion.libs.javassist.bytecode.MethodInfo;
import com.viaversion.viaversion.libs.javassist.compiler.CompileError;
import com.viaversion.viaversion.libs.javassist.compiler.Javac;
import com.viaversion.viaversion.libs.javassist.expr.Expr;

public class MethodCall
extends Expr {
    protected MethodCall(int pos, CodeIterator i2, CtClass declaring, MethodInfo m2) {
        super(pos, i2, declaring, m2);
    }

    private int getNameAndType(ConstPool cp) {
        int pos = this.currentPos;
        int c2 = this.iterator.byteAt(pos);
        int index = this.iterator.u16bitAt(pos + 1);
        if (c2 == 185) {
            return cp.getInterfaceMethodrefNameAndType(index);
        }
        return cp.getMethodrefNameAndType(index);
    }

    @Override
    public CtBehavior where() {
        return super.where();
    }

    @Override
    public int getLineNumber() {
        return super.getLineNumber();
    }

    @Override
    public String getFileName() {
        return super.getFileName();
    }

    protected CtClass getCtClass() throws NotFoundException {
        return this.thisClass.getClassPool().get(this.getClassName());
    }

    public String getClassName() {
        ConstPool cp = this.getConstPool();
        int pos = this.currentPos;
        int c2 = this.iterator.byteAt(pos);
        int index = this.iterator.u16bitAt(pos + 1);
        String cname = c2 == 185 ? cp.getInterfaceMethodrefClassName(index) : cp.getMethodrefClassName(index);
        if (cname.charAt(0) == '[') {
            cname = Descriptor.toClassName(cname);
        }
        return cname;
    }

    public String getMethodName() {
        ConstPool cp = this.getConstPool();
        int nt = this.getNameAndType(cp);
        return cp.getUtf8Info(cp.getNameAndTypeName(nt));
    }

    public CtMethod getMethod() throws NotFoundException {
        return this.getCtClass().getMethod(this.getMethodName(), this.getSignature());
    }

    public String getSignature() {
        ConstPool cp = this.getConstPool();
        int nt = this.getNameAndType(cp);
        return cp.getUtf8Info(cp.getNameAndTypeDescriptor(nt));
    }

    @Override
    public CtClass[] mayThrow() {
        return super.mayThrow();
    }

    public boolean isSuper() {
        return this.iterator.byteAt(this.currentPos) == 183 && !this.where().getDeclaringClass().getName().equals(this.getClassName());
    }

    @Override
    public void replace(String statement) throws CannotCompileException {
        String signature;
        String methodname;
        String classname;
        int opcodeSize;
        this.thisClass.getClassFile();
        ConstPool constPool = this.getConstPool();
        int pos = this.currentPos;
        int index = this.iterator.u16bitAt(pos + 1);
        int c2 = this.iterator.byteAt(pos);
        if (c2 == 185) {
            opcodeSize = 5;
            classname = constPool.getInterfaceMethodrefClassName(index);
            methodname = constPool.getInterfaceMethodrefName(index);
            signature = constPool.getInterfaceMethodrefType(index);
        } else if (c2 == 184 || c2 == 183 || c2 == 182) {
            opcodeSize = 3;
            classname = constPool.getMethodrefClassName(index);
            methodname = constPool.getMethodrefName(index);
            signature = constPool.getMethodrefType(index);
        } else {
            throw new CannotCompileException("not method invocation");
        }
        Javac jc = new Javac(this.thisClass);
        ClassPool cp = this.thisClass.getClassPool();
        CodeAttribute ca = this.iterator.get();
        try {
            CtClass[] params = Descriptor.getParameterTypes(signature, cp);
            CtClass retType = Descriptor.getReturnType(signature, cp);
            int paramVar = ca.getMaxLocals();
            jc.recordParams(classname, params, true, paramVar, this.withinStatic());
            int retVar = jc.recordReturnType(retType, true);
            if (c2 == 184) {
                jc.recordStaticProceed(classname, methodname);
            } else if (c2 == 183) {
                jc.recordSpecialProceed("$0", classname, methodname, signature, index);
            } else {
                jc.recordProceed("$0", methodname);
            }
            MethodCall.checkResultValue(retType, statement);
            Bytecode bytecode = jc.getBytecode();
            MethodCall.storeStack(params, c2 == 184, paramVar, bytecode);
            jc.recordLocalVariables(ca, pos);
            if (retType != CtClass.voidType) {
                bytecode.addConstZero(retType);
                bytecode.addStore(retVar, retType);
            }
            jc.compileStmnt(statement);
            if (retType != CtClass.voidType) {
                bytecode.addLoad(retVar, retType);
            }
            this.replace0(pos, bytecode, opcodeSize);
        }
        catch (CompileError e2) {
            throw new CannotCompileException(e2);
        }
        catch (NotFoundException e3) {
            throw new CannotCompileException(e3);
        }
        catch (BadBytecode e4) {
            throw new CannotCompileException("broken method");
        }
    }
}

