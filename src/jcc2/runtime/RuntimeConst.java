/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jcc2.runtime;

/**
 *
 * @author note173@gmail.com
 */

public interface RuntimeConst
{
    public static final int NOP = 1;
    public static final int ISTORE = 2;
    public static final int LSTORE = 3;
    public static final int FSTORE = 4;
    public static final int DSTORE = 5;
    public static final int ASTORE = 6;
    public static final int ILOAD = 7;
    public static final int LLOAD = 8;
    public static final int FLOAD = 9;
    public static final int DLOAD = 10;
    public static final int ALOAD = 11;

    public static final int INVOKE = 12;
    public static final int INVOKE_LIB = 13;
    public static final int RET_V = 14;
    public static final int RET = 15;

    public static final int ANEWARRAY = 16;
    public static final int NEWARRAY = 17;
    public static final int T_BYTE = 1;
    public static final int T_CHAR = 2;
    public static final int T_SHORT = 3;
    public static final int T_INT = 4;
    public static final int T_LONG = 5;
    public static final int T_FLOAT = 6;
    public static final int T_DOUBLE = 7;
    public static final int MULTIANEWARRAY = 18;
    public static final int ARRAYLENGTH = 19;

    public static final int IALOAD = 20;
    public static final int BALOAD = 21;
    public static final int CALOAD = 22;
    public static final int SALOAD = 23;
    public static final int LALOAD = 24;
    public static final int FALOAD = 25;
    public static final int DALOAD = 26;
    public static final int AALOAD = 27;

    public static final int IASTORE = 28;
    public static final int BASTORE = 29;
    public static final int CASTORE = 30;
    public static final int SASTORE = 31;
    public static final int LASTORE = 32;
    public static final int FASTORE = 33;
    public static final int DASTORE = 34;
    public static final int AASTORE = 35;

    public static final int DUP = 36;
    public static final int DUP2 = 37;
    public static final int DUP_X1 = 38;
    public static final int DUP_X2 = 39;
    public static final int DUP2_X1 = 40;
    public static final int DUP2_X2 = 41;

    public static final int SWAP = 42;

    public static final int LDC = 43;
    public static final int ICONST = 44;
    public static final int ACONST_NULL = 45;

    public static final int POP = 46;
    public static final int POP2 = 47;

    public static final int IAND = 48;
    public static final int IOR = 49;
    public static final int IXOR = 50;
    public static final int LAND = 51;
    public static final int LOR = 52;
    public static final int LXOR = 53;

    public static final int IEQ = 54;
    public static final int LEQ = 55;
    public static final int AEQ = 56;
    public static final int FEQ = 57;
    public static final int DEQ = 58;

    public static final int IGT = 59;
    public static final int ILT = 60;
    public static final int IGE = 61;
    public static final int ILE = 62;
    public static final int LGT = 63;
    public static final int LLT = 64;
    public static final int LGE = 65;
    public static final int LLE = 66;
    public static final int FGT = 67;
    public static final int FLT = 68;
    public static final int FGE = 69;
    public static final int FLE = 70;
    public static final int DGT = 71;
    public static final int DLT = 72;
    public static final int DGE = 73;
    public static final int DLE = 74;

    public static final int ISHL = 75;
    public static final int ISHR = 76;
    public static final int IUSHR = 77;
    public static final int LSHL = 78;
    public static final int LSHR = 79;
    public static final int LUSHR = 80;

    public static final int IADD = 81;
    public static final int ISUB = 82;
    public static final int IMUL = 83;
    public static final int IDIV = 84;
    public static final int IREM = 85;
    public static final int LADD = 86;
    public static final int LSUB = 87;
    public static final int LMUL = 88;
    public static final int LDIV = 89;
    public static final int LREM = 90;

    public static final int FADD = 91;
    public static final int FSUB = 92;
    public static final int FMUL = 93;
    public static final int FDIV = 94;
    public static final int DADD = 95;
    public static final int DSUB = 96;
    public static final int DMUL = 97;
    public static final int DDIV = 98;

    public static final int I2B = 99;
    public static final int I2C = 100;
    public static final int I2S = 101;
    public static final int I2L = 102;
    public static final int I2F = 103;
    public static final int I2D = 104;
    public static final int L2I = 105;
    public static final int L2F = 106;
    public static final int L2D = 107;
    public static final int F2D = 108;
    public static final int F2I = 109;
    public static final int F2L = 110;
    public static final int D2I = 111;
    public static final int D2F = 112;
    public static final int D2L = 113;

    public static final int ITOS = 114;
    public static final int LTOS = 115;
    public static final int FTOS = 116;
    public static final int DTOS = 117;
    public static final int CONCATSTR = 118;
    public static final int INEG = 119;
    public static final int LNEG = 120;
    public static final int FNEG = 121;
    public static final int DNEG = 122;
    public static final int IINC = 123;
    public static final int LINC = 124;
    public static final int INSTANCEOF = 125;

    public static final int LIBRARY = 126;

    public static final int GOTO = 127;
    public static final int IFEQ = 128;
    public static final int IFNE = 129;
    public static final int IFGT = 130;
    public static final int IFLT = 131;
    public static final int IFGE = 132;
    public static final int IFLE = 133;

    public static final int GETGLOBAL = 134;
    public static final int SETGLOBAL = 135;

    public static final int INVOKE_MEMBER = 136;

    public static final int ABYTE_TO_STR = 137;
    public static final int STR_TO_ABYTE = 148;
    public static final int STR_SUBSTR1 = 149;
    public static final int STR_SUBSTR2 = 150;
    public static final int STR_CHARAT = 151;
    public static final int STR_INDEXOF1 = 152;
    public static final int STR_INDEXOF2 = 153;
    public static final int STR_INDEXOF3 = 154;
    public static final int STR_INDEXOF4 = 155;
    public static final int STR_LASTINDEXOF1 = 156;
    public static final int STR_LASTINDEXOF2 = 157;
    public static final int STR_LASTINDEXOF3 = 158;
    public static final int STR_LASTINDEXOF4 = 159;
    public static final int STR_LENGTH = 160;
    public static final int STR_REPLACE = 161;
    public static final int STR_STARTSWITH = 162;
    public static final int STR_ENDSWITH = 163;
    public static final int STR_TRIM = 164;

    public static final int OBJ_EQUALS = 165;
}
