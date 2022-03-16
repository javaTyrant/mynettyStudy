package io.netty.util.lutest;

import java.util.Arrays;

/**
 * @author lufengxiang
 * @since 2022/3/1
 **/
public class MyCollector {
    public int findMin(int[] nums) {
        int i = 0;
        int j = nums.length - 1;
        while (i < j) {
            int mid = (j - i) / 2 + i;
            if (nums[mid] > nums[j]) {
                i = mid + 1;
            } else if (nums[mid] < nums[j]) {
                j = mid;
            }
        }
        return nums[i];
    }

    public static void main(String[] args) {
        int[] arr = {5, 7, 7, 8, 8, 10};
        MyCollector solution = new MyCollector();
        int[] range = solution.searchRange(arr, 8);
        System.out.println(Arrays.toString(range));
        int[] arr1 = {4, 5, 6, 7, 0, 1, 2};
        solution.search(arr1, 0);
    }

    //
    //[5,7,7,8,8,10], target = 8,[3,4]
    public int[] searchRange(int[] nums, int target) {
        //第一个大于等于target的下标
        int leftIdx = binarySearch(nums, target, true);
        //第一个大于target的下标-1
        int rightIdx = binarySearch(nums, target, false) - 1;
        if (leftIdx <= rightIdx) {
            return new int[]{leftIdx, rightIdx};
        }
        return new int[]{-1, -1};
    }

    public int binarySearch(int[] nums, int target, boolean lower) {
        //
        int left = 0, right = nums.length - 1, ans = nums.length;
        //
        while (left <= right) {
            int mid = (left + right) / 2;
            //
            if (nums[mid] > target || (lower && nums[mid] >= target)) {
                //放弃右边,
                right = mid - 1;
                ans = mid;
            } else {
                //放弃左边
                left = mid + 1;
            }
        }
        return ans;
    }

    //只旋转了一次,一定有部分是有序的
    public int search(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;
        //为什么不能left < right,什么情况必须<呢?
        while (left <= right) {
            int mid = (right - left) / 2 + left;
            if (nums[mid] == target) {
                return mid;
            } else if (nums[mid] < nums[right]) {//右边有序,nums[right]最大,nums[mid]最小.
                //在有序的右边吗?
                if (nums[mid] < target && target <= nums[right]) {//放弃左边
                    left = mid + 1;
                } else {//放弃右边
                    right = mid - 1;
                }
            } else if (nums[mid] >= nums[right]) {//左边有序
                //在有序的左边吗?nums[mid]最大,nums[left]最小.
                if (target >= nums[left] && target < nums[mid]) {//放弃右边
                    right = mid - 1;
                } else {//放弃左边
                    left = mid + 1;
                }
            }
        }
        return -1;
    }

    //整数反转
    public int reverse(int x) {
        //防止溢出
        long n = 0;
        //x不断的/10
        while (x != 0) {
            n = n * 10 + x % 10;
            x = x / 10;
        }
        return (int) n == n ? (int) n : 0;
    }

    //无重复字符的最长子串
    public int lengthOfLongestSubstring(String s) {
        return 0;
    }
}
