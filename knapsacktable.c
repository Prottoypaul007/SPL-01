#include<stdio.h>

int max(int a, int b)
{
    if(a > b)
    {
        return a;
    }
    else 
    {
        return b;
    }
}

int main()
{
    int weight[] = {3, 4, 6, 5};
    int profit[] = {2, 3, 1, 4};
    int total = 8;
    int i, j;
    int leng = sizeof(weight) / sizeof(weight[0]);
    int map[leng][2]; 
    
    for (int i = 0; i < leng; i++) 
    {
        map[i][0] = weight[i];
        map[i][1] = profit[i];
    }

    for (int i = 0; i < leng - 1; i++) {
        for (int j = i + 1; j < leng; j++) {
            if (map[i][0] > map[j][0]) {
                int temp0 = map[i][0];
                int temp1 = map[i][1];
                map[i][0] = map[j][0];
                map[i][1] = map[j][1];
                map[j][0] = temp0;
                map[j][1] = temp1;
            }
        }
    }

    for (int i = 0; i < leng; i++) 
    {
        weight[i] = map[i][0];
        profit[i] = map[i][1];
    }
        int knap[leng + 1][total + 1];
        for(i = 0; i <= leng; i++)
    {
        for(j = 0; j <= total; j++)
        {
            if(i == 0 || j == 0)
            {
                knap[i][j] = 0;
            }
            else if(j >= weight[i - 1])
            {
                knap[i][j] = max(knap[i - 1][j], knap[i - 1][j - weight[i - 1]] + profit[i - 1]);
            }
            else
            {
                knap[i][j] = knap[i - 1][j];
            }
        }
    }
    
    printf("\nKnapsack DP Table:\n");
    printf("    ");
    for(j = 0; j <= total; j++) {
        printf("%3d ", j);
    }
    printf("\n");
    
    for(i = 0; i <= leng; i++) {
        printf("%2d ", i);
        for(j = 0; j <= total; j++) {
            printf("%3d ", knap[i][j]);
        }
        printf("\n");
    }
    
    printf("\nMaximum profit: %d\n", knap[leng][total]);
    
    return 0;
}
