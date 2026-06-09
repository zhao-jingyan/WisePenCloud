# WisePenCloud 后端编码约定

本文档约束 Controller、Service、Mapper、注释、命名和性能规则。安全上下文和入口安全规则是硬约束，优先级高于局部代码习惯。

## 1. 分层职责

- Controller 负责 HTTP 入口、`@CheckLogin` / `@CheckRole`、读取 `SecurityContextHolder`、参数校验、OpenAPI 文案和 `R` / `PageR` 响应包装。
- Service 负责业务流程、事务边界、业务状态约束、资源归属校验、跨服务编排和错误码抛出。
- Mapper / Repository 负责数据访问，不写业务决策，不读取安全上下文。
- Entity、DTO、Request、Response、Message 只表达数据结构，不写业务流程。

## 2. 安全上下文硬规则

- `@CheckLogin` / `@CheckRole` 只放 Controller 类或 Controller 方法。
- Service 不得调用或依赖 `SecurityContextHolder`。
- 当前用户 ID、身份类型、小组角色映射等上下文必须在 Controller 读取，再作为明确参数传给 Service。
- Service 内需要权限判断时，使用参数中的用户或角色信息做业务归属、资源权限和状态约束。
- 不要把安全切面注解放到 Service 上来弥补 Controller 漏标；应修正 Controller 入口。

## 3. Controller 写法

- Controller 保持薄层，但可以承担入口级安全上下文读取、参数整理和明确的业务入口断言。
- 请求参数使用 Jakarta Validation 或 Spring Validation 做基础校验。
- 涉及副作用的 Controller 方法按日志治理规则使用 `@Log`，除非明确属于白名单。
- 不在 Controller 中吞异常；业务失败抛 `ServiceException` 或现有权限异常体系，统一异常处理器负责响应。
- 不在 Controller 中重复打印 HTTP 异常出口日志。

## 4. Service 写法

- Service 方法参数应表达业务语义，例如 `operatorUserId`、`groupRole`、`resourceId`，不要传含糊的上下文对象。
- 事务边界优先放在 Service 层；不要把事务逻辑散落在 Controller。
- 跨服务调用失败需要降级时，必须说明降级理由、影响范围和错误码或日志策略。
- 写兜底、降级、兼容分支前必须先探索现有调用链和真实失败场景，确认必要性。
- 不要为了“看起来稳”堆叠多层 null 兜底、默认值兜底或吞异常逻辑。

## 5. 错误与异常

- 业务失败使用 `ServiceException` 携带真实存在的 `IResult` 枚举。
- 权限认证失败使用现有 `PermissionException` / `PermissionError` 体系。
- 不硬编码裸数字错误码，不返回临时字符串错误，不编造不存在的错误枚举。
- catch 后继续抛出时默认不重复打日志；降级返回或吞掉异常时必须按日志治理文档记录原因。

## 6. 注释

- 注释语言使用中文。
- 只为复杂业务规则、状态机迁移、跨服务边界、非显然兜底、数据一致性约束添加必要注释。
- 不写“把 A 赋值给 B”这类重复代码表面的注释。
- TODO 必须说明后续要补什么，不写无责任人的空泛 TODO。

## 7. 命名与函数拆分

- 变量名必须言之有物，优先表达领域含义，例如 `operatorUserId`、`targetResourceId`、`groupRole`。
- 避免 `data`、`result1`、`temp`、`obj`、`map` 这类缺少业务语义的命名，除非作用域极小且含义显然。
- 不拆三四行且只有一两个调用者的零碎小函数。
- 拆分函数应服务于清晰命名、真实复用、复杂度下降或隔离副作用。
- 不为了追求“工具函数化”把顺序业务流程拆到难以阅读。

## 8. 性能规则

- 避免 N+1 查询，尤其不要在遍历结果时逐条查数据库。
- 避免循环中远程调用、循环中对象存储访问、循环中高成本序列化或高频日志。
- 避免无界分页和不设上限的列表查询。
- 避免重复查询同一实体；在方法内部可使用局部变量或局部 Map 缓存已查结果。
- 批量场景优先批量查询、预聚合、一次性取回依赖数据，再在内存中组装。
- 如果确实需要逐条处理，必须确认数据规模、调用频率和失败影响，并在代码或说明中交代理由。

## 9. Review 检查清单

- Controller 是否承担了安全注解和 `SecurityContextHolder` 读取。
- Service 是否完全不依赖 `SecurityContextHolder`。
- 变量名、方法名是否表达业务含义。
- 是否避免了无意义小函数和过度兜底。
- 是否存在 N+1 查询、循环远程调用、无界分页或重复查询。
