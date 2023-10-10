/*
 * Decompiled with CFR 0.152.
 */
package com.viaversion.viaversion.libs.javassist.bytecode.annotation;

import com.viaversion.viaversion.libs.javassist.ClassPool;
import com.viaversion.viaversion.libs.javassist.CtClass;
import com.viaversion.viaversion.libs.javassist.NotFoundException;
import com.viaversion.viaversion.libs.javassist.bytecode.AnnotationDefaultAttribute;
import com.viaversion.viaversion.libs.javassist.bytecode.ClassFile;
import com.viaversion.viaversion.libs.javassist.bytecode.MethodInfo;
import com.viaversion.viaversion.libs.javassist.bytecode.annotation.Annotation;
import com.viaversion.viaversion.libs.javassist.bytecode.annotation.MemberValue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class AnnotationImpl
implements InvocationHandler {
    private static final String JDK_ANNOTATION_CLASS_NAME = "java.lang.annotation.Annotation";
    private static Method JDK_ANNOTATION_TYPE_METHOD = null;
    private Annotation annotation;
    private ClassPool pool;
    private ClassLoader classLoader;
    private transient Class<?> annotationType;
    private transient int cachedHashCode = Integer.MIN_VALUE;

    public static Object make(ClassLoader cl, Class<?> clazz, ClassPool cp, Annotation anon) throws IllegalArgumentException {
        AnnotationImpl handler = new AnnotationImpl(anon, cp, cl);
        return Proxy.newProxyInstance(cl, new Class[]{clazz}, handler);
    }

    private AnnotationImpl(Annotation a2, ClassPool cp, ClassLoader loader) {
        this.annotation = a2;
        this.pool = cp;
        this.classLoader = loader;
    }

    public String getTypeName() {
        return this.annotation.getTypeName();
    }

    private Class<?> getAnnotationType() {
        if (this.annotationType == null) {
            String typeName = this.annotation.getTypeName();
            try {
                this.annotationType = this.classLoader.loadClass(typeName);
            }
            catch (ClassNotFoundException e2) {
                NoClassDefFoundError error = new NoClassDefFoundError("Error loading annotation class: " + typeName);
                error.setStackTrace(e2.getStackTrace());
                throw error;
            }
        }
        return this.annotationType;
    }

    public Annotation getAnnotation() {
        return this.annotation;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MemberValue mv;
        String name = method.getName();
        if (Object.class == method.getDeclaringClass()) {
            if ("equals".equals(name)) {
                Object obj = args[0];
                return this.checkEquals(obj);
            }
            if ("toString".equals(name)) {
                return this.annotation.toString();
            }
            if ("hashCode".equals(name)) {
                return this.hashCode();
            }
        } else if ("annotationType".equals(name) && method.getParameterTypes().length == 0) {
            return this.getAnnotationType();
        }
        if ((mv = this.annotation.getMemberValue(name)) == null) {
            return this.getDefault(name, method);
        }
        return mv.getValue(this.classLoader, this.pool, method);
    }

    private Object getDefault(String name, Method method) throws ClassNotFoundException, RuntimeException {
        String classname = this.annotation.getTypeName();
        if (this.pool != null) {
            try {
                AnnotationDefaultAttribute ainfo;
                CtClass cc = this.pool.get(classname);
                ClassFile cf = cc.getClassFile2();
                MethodInfo minfo = cf.getMethod(name);
                if (minfo != null && (ainfo = (AnnotationDefaultAttribute)minfo.getAttribute("AnnotationDefault")) != null) {
                    MemberValue mv = ainfo.getDefaultValue();
                    return mv.getValue(this.classLoader, this.pool, method);
                }
            }
            catch (NotFoundException e2) {
                throw new RuntimeException("cannot find a class file: " + classname);
            }
        }
        throw new RuntimeException("no default value: " + classname + "." + name + "()");
    }

    public int hashCode() {
        if (this.cachedHashCode == Integer.MIN_VALUE) {
            int hashCode = 0;
            this.getAnnotationType();
            Method[] methods = this.annotationType.getDeclaredMethods();
            for (int i2 = 0; i2 < methods.length; ++i2) {
                String name = methods[i2].getName();
                int valueHashCode = 0;
                MemberValue mv = this.annotation.getMemberValue(name);
                Object value = null;
                try {
                    if (mv != null) {
                        value = mv.getValue(this.classLoader, this.pool, methods[i2]);
                    }
                    if (value == null) {
                        value = this.getDefault(name, methods[i2]);
                    }
                }
                catch (RuntimeException e2) {
                    throw e2;
                }
                catch (Exception e3) {
                    throw new RuntimeException("Error retrieving value " + name + " for annotation " + this.annotation.getTypeName(), e3);
                }
                if (value != null) {
                    valueHashCode = value.getClass().isArray() ? AnnotationImpl.arrayHashCode(value) : value.hashCode();
                }
                hashCode += 127 * name.hashCode() ^ valueHashCode;
            }
            this.cachedHashCode = hashCode;
        }
        return this.cachedHashCode;
    }

    private boolean checkEquals(Object obj) throws Exception {
        InvocationHandler ih;
        if (obj == null) {
            return false;
        }
        if (obj instanceof Proxy && (ih = Proxy.getInvocationHandler(obj)) instanceof AnnotationImpl) {
            AnnotationImpl other = (AnnotationImpl)ih;
            return this.annotation.equals(other.annotation);
        }
        Class otherAnnotationType = (Class)JDK_ANNOTATION_TYPE_METHOD.invoke(obj, new Object[0]);
        if (!this.getAnnotationType().equals(otherAnnotationType)) {
            return false;
        }
        Method[] methods = this.annotationType.getDeclaredMethods();
        for (int i2 = 0; i2 < methods.length; ++i2) {
            String name = methods[i2].getName();
            MemberValue mv = this.annotation.getMemberValue(name);
            Object value = null;
            Object otherValue = null;
            try {
                if (mv != null) {
                    value = mv.getValue(this.classLoader, this.pool, methods[i2]);
                }
                if (value == null) {
                    value = this.getDefault(name, methods[i2]);
                }
                otherValue = methods[i2].invoke(obj, new Object[0]);
            }
            catch (RuntimeException e2) {
                throw e2;
            }
            catch (Exception e3) {
                throw new RuntimeException("Error retrieving value " + name + " for annotation " + this.annotation.getTypeName(), e3);
            }
            if (value == null && otherValue != null) {
                return false;
            }
            if (value == null || value.equals(otherValue)) continue;
            return false;
        }
        return true;
    }

    private static int arrayHashCode(Object object) {
        if (object == null) {
            return 0;
        }
        int result = 1;
        Object[] array = (Object[])object;
        for (int i2 = 0; i2 < array.length; ++i2) {
            int elementHashCode = 0;
            if (array[i2] != null) {
                elementHashCode = array[i2].hashCode();
            }
            result = 31 * result + elementHashCode;
        }
        return result;
    }

    static {
        try {
            Class<?> clazz = Class.forName(JDK_ANNOTATION_CLASS_NAME);
            JDK_ANNOTATION_TYPE_METHOD = clazz.getMethod("annotationType", null);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

