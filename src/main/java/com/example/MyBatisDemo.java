package com.example;

import com.example.entity.Order;
import com.example.entity.User;
import com.example.entity.UserQuery;
import com.example.service.AccountService;
import com.example.service.OrderService;
import com.example.service.UserService;
import com.example.service.impl.AccountServiceImpl;
import com.example.service.impl.OrderServiceImpl;
import com.example.service.impl.UserServiceImpl;
import com.example.util.DatabaseInit;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MyBatisDemo {

    private static final Logger log = LoggerFactory.getLogger(MyBatisDemo.class);

    public static void main(String[] args) {
        DatabaseInit.init();
        AccountService accountService = new AccountServiceImpl();
        OrderService orderService = new OrderServiceImpl();
        UserService userService = new UserServiceImpl();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println();
            System.out.println("===== 用户管理系统 =====");
            System.out.println("1. 查询所有用户");
            System.out.println("2. 根据 ID 查询用户");
            System.out.println("3. 控制台新增用户");
            System.out.println("4. 从文件批量导入");
            System.out.println("5. 动态条件查询");
            System.out.println("6. 分页查询（PageHelper）");
            System.out.println("7. 账户转账（事务）");
            System.out.println("8. 查看用户+订单（多表）");
            System.out.println("9. 查看订单（按用户）");
            System.out.println("0. 退出");
            System.out.print("请选择操作：");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    List<User> users = userService.selectAll();
                    System.out.println("共 " + users.size() + " 条记录：");
                    users.forEach(u -> System.out.println("  " + u));
                    break;
                case "2":
                    System.out.print("请输入用户 ID：");
                    try {
                        int id = Integer.parseInt(scanner.nextLine().trim());
                        User user = userService.selectById(id);
                        if (user != null) {
                            System.out.println("查询结果：" + user);
                        } else {
                            System.out.println("未找到 ID=" + id + " 的用户");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("请输入有效的数字 ID");
                    }
                    break;
                case "3":
                    insertFromConsole(scanner, userService);
                    break;
                case "4":
                    importFromFile(scanner, userService);
                    break;
                case "5":
                    dynamicQuery(scanner, userService);
                    break;
                case "6":
                    pageQuery(scanner, userService);
                    break;
                case "7":
                    transfer(scanner, accountService);
                    break;
                case "8":
                    userWithOrders(scanner, userService);
                    break;
                case "9":
                    ordersByUser(scanner, orderService);
                    break;
                case "0":
                    System.out.println("再见！");
                    return;
                default:
                    System.out.println("无效选项，请重新输入");
            }
        }
    }

    private static void insertFromConsole(Scanner scanner, UserService userService) {
        System.out.print("用户名：");
        String username = scanner.nextLine().trim();
        System.out.print("密码：");
        String password = scanner.nextLine().trim();
        System.out.print("邮箱：");
        String email = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            System.out.println("用户名、密码、邮箱不能为空");
            return;
        }

        User user = new User(username, password, email);
        int rows = userService.insertUser(user);
        System.out.println("新增成功，影响行数：" + rows + "，ID：" + user.getId());
    }

    private static void importFromFile(Scanner scanner, UserService userService) {
        System.out.print("请输入文件路径（支持 CSV，格式：username,password,email）：");
        String filePath = scanner.nextLine().trim();

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("文件不存在：" + filePath);
            return;
        }

        List<User> users = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().startsWith("username")) {
                        continue;
                    }
                }
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.out.println("跳过格式错误的行：" + line);
                    continue;
                }
                users.add(new User(parts[0].trim(), parts[1].trim(), parts[2].trim()));
            }
        } catch (IOException e) {
            System.out.println("读取文件失败：" + e.getMessage());
            return;
        }

        if (users.isEmpty()) {
            System.out.println("文件中没有有效的用户数据");
            return;
        }

        int rows = userService.insertBatch(users);
        System.out.println("批量导入完成，共导入 " + rows + " 条记录");
    }

    private static void dynamicQuery(Scanner scanner, UserService userService) {
        System.out.print("用户名关键字（可空）：");
        String username = scanner.nextLine().trim();
        UserQuery query = new UserQuery();
        query.setUsername(username);
        List<User> users = userService.selectByCondition(query);
        System.out.println("共 " + users.size() + " 条记录：");
        users.forEach(u -> System.out.println("  " + u));
    }

    private static void pageQuery(Scanner scanner, UserService userService) {
        System.out.print("页码：");
        int pageNum = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("每页条数：");
        int pageSize = Integer.parseInt(scanner.nextLine().trim());
        PageInfo<User> page = userService.selectPageByHelper(pageNum, pageSize);
        System.out.println("第 " + page.getPageNum() + " 页，共 " + page.getTotal() + " 条：");
        page.getList().forEach(u -> System.out.println("  " + u));
    }

    private static void transfer(Scanner scanner, AccountService accountService) {
        System.out.print("转出账户ID：");
        int fromId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("转入账户ID：");
        int toId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("金额：");
        double amount = Double.parseDouble(scanner.nextLine().trim());
        try {
            accountService.transfer(fromId, toId, amount);
            System.out.println("转账成功");
        } catch (RuntimeException e) {
            System.out.println("转账失败：" + e.getMessage());
        }
    }

    private static void userWithOrders(Scanner scanner, UserService userService) {
        System.out.print("用户ID：");
        int id = Integer.parseInt(scanner.nextLine().trim());
        User user = userService.selectUserWithOrders(id);
        if (user == null) {
            System.out.println("用户不存在");
            return;
        }
        System.out.println(user);
        if (user.getOrders() != null) {
            user.getOrders().forEach(o -> System.out.println("  订单: " + o));
        }
    }

    private static void ordersByUser(Scanner scanner, OrderService orderService) {
        System.out.print("用户ID：");
        int userId = Integer.parseInt(scanner.nextLine().trim());
        List<Order> orders = orderService.selectByUserId(userId);
        System.out.println("共 " + orders.size() + " 条订单：");
        orders.forEach(o -> System.out.println("  " + o));
    }
}
