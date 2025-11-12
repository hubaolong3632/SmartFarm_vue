# MyBatis-Plus 迁移说明

项目已从 JPA 迁移到 MyBatis-Plus。

## 主要变更

### 1. 依赖变更
- 移除了 `spring-boot-starter-data-jpa`
- 添加了 `mybatis-plus-boot-starter` (3.5.5)

### 2. 配置变更
- 移除了 JPA 相关配置
- 添加了 MyBatis-Plus 配置（`application.yml`）
- 添加了 MyBatis-Plus 配置类（`MyBatisPlusConfig.java`）

### 3. 实体类变更
- 移除了 JPA 注解（`@Entity`, `@Table`, `@Column`, `@ManyToOne` 等）
- 使用 MyBatis-Plus 注解（`@TableName`, `@TableId`）
- 移除了关联对象，改为使用 ID 字段（如 `plotId`, `recipeId`）

### 4. 数据访问层变更
- 将 `Repository` 接口改为 `Mapper` 接口
- 所有 Mapper 继承 `BaseMapper<T>`
- 自定义查询方法使用 `@Select`, `@Update` 等注解

### 5. Service 层变更
- 使用 `mapper.insert()`, `mapper.updateById()`, `mapper.selectById()` 等方法
- 使用 `LambdaQueryWrapper` 进行条件查询
- 手动设置 `createdAt` 和 `updatedAt` 字段

### 6. Controller 层变更
- 将 Repository 注入改为 Mapper 注入
- 使用 Mapper 方法替代 Repository 方法

## 主要差异

### 实体类
**JPA 方式：**
```java
@Entity
@Table(name = "plot_assignments")
public class PlotAssignment {
    @ManyToOne
    @JoinColumn(name = "plot_id")
    private Plot plot;
}
```

**MyBatis-Plus 方式：**
```java
@TableName("plot_assignments")
public class PlotAssignment {
    private Integer plotId;
}
```

### 数据访问
**JPA 方式：**
```java
public interface PlotRepository extends JpaRepository<Plot, Integer> {
    Optional<Plot> findByPlotNumber(Integer plotNumber);
}
```

**MyBatis-Plus 方式：**
```java
@Mapper
public interface PlotMapper extends BaseMapper<Plot> {
    @Select("SELECT * FROM plots WHERE plot_number = #{plotNumber}")
    Plot findByPlotNumber(Integer plotNumber);
}
```

### Service 层
**JPA 方式：**
```java
recipeRepository.save(recipe);
recipeRepository.findById(id);
recipeRepository.findAll();
```

**MyBatis-Plus 方式：**
```java
recipeMapper.insert(recipe);
recipeMapper.selectById(id);
recipeMapper.selectList(null);
```

## 注意事项

1. **时间字段**：需要手动设置 `createdAt` 和 `updatedAt`，不再自动填充
2. **关联查询**：不再使用 JPA 的关联查询，需要手动 JOIN 或多次查询
3. **批量操作**：批量插入需要循环调用 `insert()` 方法
4. **分页查询**：使用 MyBatis-Plus 的分页插件（已配置）

## 优势

1. **性能更好**：直接使用 SQL，性能更优
2. **SQL 可控**：可以精确控制 SQL 语句
3. **学习成本低**：SQL 语法更直观
4. **功能强大**：MyBatis-Plus 提供了丰富的 CRUD 方法

## 后续优化建议

1. 可以创建 XML Mapper 文件处理复杂查询
2. 可以使用 MyBatis-Plus 的自动填充功能处理时间字段
3. 可以使用 MyBatis-Plus 的逻辑删除功能
4. 可以使用 MyBatis-Plus 的乐观锁功能

