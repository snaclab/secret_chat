package com.tiemens.secretshare.math.matrix;

import java.io.PrintStream;

public abstract class NumberMatrix <E extends Number>
{
    protected E[][] matrix;

    /** Abstract method to create a matrix */
    protected abstract E[][] create(int height, int width);

    /** Abstract method for defining zero for the matrix element */
    protected abstract E zero();

    protected abstract E one();

    /** Abstract method for creating a cell with equivalent value "v" */
    protected abstract E createValue(int v);

    /** Abstract method for adding two elements of the matrices */
    protected abstract E add(E o1, E o2);

    /** Abstract method for subtracting elements of the matrices */
    protected abstract E subtract(E o1, E o2);

    /** Abstract method for multiplying two elements of the matrices */
    protected abstract E multiply(E o1, E o2);

    /** Abstract */
    protected abstract E reciprocal(E o1);

    /** Abstract */
    protected abstract E negate(E o1);



    public E[][] getArray()
    {
        return matrix;
    }

    protected NumberMatrix(E[][] in)
    {
        matrix = in;
    }

    public NumberMatrix(int height, int width)
    {
        matrix = create(height, width);
    }

    public int getHeight()
    {
        return matrix.length;
    }

    public int getWidth()
    {
        return matrix[0].length;
    }

    public final boolean isValueOne(E other)
    {
        return one().equals(other);
    }

    public void fill(int j, int ... rowsandcols)
    {
        if ((rowsandcols.length % j) != 0)
        {
            throw new ArithmeticException("array size must be evenly divisible by j(" + j + ")");
        }
        int i = rowsandcols.length / j;
        if (i != getHeight())
        {
            throw new ArithmeticException("refusing to resize height from " + getHeight() + " to " + i);
        }
        if (j != getWidth())
        {
            throw new ArithmeticException("refusing to resize width from " + getWidth() + " to " + j);
        }
        for (int r = 0; r < i; r++)
        {
            for (int c = 0; c < j; c++)
            {
                int index = r * j + c;
                matrix[r][c] = createValue(rowsandcols[index]);
            }
        }
    }


    /** Add two matrices */
    public E[][] addMatrix(E[][] matrix1, E[][] matrix2)
    {
        // Check bounds of the two matrices
        if ((matrix1.length != matrix2.length) ||
            (matrix1[0].length != matrix2[0].length))
        {
            throw new RuntimeException("The matrices do not have the same size");
        }

        E[][] result = //(E[][])new Number[matrix1.length][matrix1[0].length];
                create(matrix1.length, matrix1[0].length);

        // Perform addition
        for (int i = 0; i < result.length; i++)
        {
            for (int j = 0; j < result[i].length; j++)
            {
                result[i][j] = add(matrix1[i][j], matrix2[i][j]);
            }
        }

        return result;
    }

    /** Multiply two matrices */
    public E[][] multiplyMatrix(E[][] matrix1, E[][] matrix2)
    {
        // Check bounds
        if (matrix1[0].length != matrix2.length)
        {
            throw new RuntimeException("The matrices do not have compatible size");
        }

        // Create result matrix
        E[][] result = //(E[][]) new Number[matrix1.length][matrix2[0].length];
                create(matrix1.length, matrix2[0].length);

        // Perform multiplication of two matrices
        for (int i = 0; i < result.length; i++)
        {
            for (int j = 0; j < result[0].length; j++)
            {
                result[i][j] = zero();

                for (int k = 0; k < matrix1[0].length; k++)
                {
                    result[i][j] = add(result[i][j],
                                   multiply(matrix1[i][k], matrix2[k][j]));
                }
            }
        }

        return result;
    }
    //  det(a(rs) a(rj)
    //      a(is) a(ij))  = a(rs)*a(ij)  -  a(is)*a(rj)
    //
    // det(c11 c12)                        ( a b )
    //    (c21 c22)  = c11*c22 - c21*c12   ( c d )
    //   called as det(0, 0, 1, 1) -- index starts at 0
    public E determinant(E[][] matrix, int r, int s, int i, int j)
    {
        E a, b, c, d;
        a = matrix[r][s];
        b = matrix[r][j];
        c = matrix[i][s];
        d = matrix[i][j];
        E ad  = multiply(a, d); // matrix[r][s], matrix[i][j]);
        E bc = multiply(b, c); // matrix[i][s], matrix[r][j]);
        E ret = subtract(ad, bc);

        return ret;
    }

    public void printResult(PrintStream out)
    {
        printResult(getArray(), out);
    }

    public static void print(String string, Number[][] matrix2, PrintStream out)
    {
        if (out == null)
        {
            return;
        }
        out.println(string);
        printResult(matrix2, out);
    }

    public static void printResult(Number[][] m1, PrintStream out)
    {
        if (out == null)
        {
            return;
        }
        for (int i = 0; i < m1.length; i++)
        {
            for (int j = 0; j < m1[0].length; j++)
            {
                out.print(" " + m1[i][j]);
            }
            out.println("");
        }
    }

    /** Print matrices, the operator, and their operation result */
    public static void printResult(Number[][] m1, Number[][] m2, Number[][] m3, char op, PrintStream out)
    {
        for (int i = 0; i < m1.length; i++)
        {
            for (int j = 0; j < m1[0].length; j++)
            {
                out.print(" " + m1[i][j]);
            }

            if (i == m1.length / 2)
            {
                out.print("  " + op + "  ");
            }
            else
            {
                out.print("     ");
            }

            for (int j = 0; j < m2.length; j++)
            {
                out.print(" " + m2[i][j]);
            }

            if (i == m1.length / 2)
            {
                out.print("  =  ");
            }
            else
            {
                out.print("     ");
            }

            for (int j = 0; j < m3.length; j++)
            {
                out.print(m3[i][j] + " ");
            }

            out.println();
        }
    }
}
