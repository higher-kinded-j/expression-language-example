# Optics Blog Series
## [Article 1](blog/article-1-the-immutability-gap.md)

- **[NestedUpdateProblem](../src/main/java/org/higherkindedj/article1/problem/NestedUpdateProblem.java)**: The problem with nested updates of immutable data
```bash
./gradlew :run -PmainClass=org.higherkindedj.article1.problem.NestedUpdateProblem
```
- **[OpticsSolution](../src/main/java/org/higherkindedj/article1/solution/OpticsSolution.java)**: How Optics help solve this
```bash
./gradlew :run -PmainClass=org.higherkindedj.article1.solution.OpticsSolution

```
## [Article 2](blog/article-2-optics-fundamentals.md)
- **[LensDemo](../src/main/java/org/higherkindedj/article2/demo/LensDemo.java)**: Basic lens operations and composition
```bash
./gradlew :run -PmainClass=org.higherkindedj.article2.demo.LensDemo
```
- **[PrismDemo](../src/main/java/org/higherkindedj/article2/demo/PrismDemo.java)**: Prism operations and type-safe downcasting
```bash
./gradlew :run -PmainClass=org.higherkindedj.article2.demo.PrismDemo
```
- **[TraversalDemo](../src/main/java/org/higherkindedj/article2/demo/TraversalDemo.java)**: List traversals and filtering
```bash
./gradlew :run -PmainClass=org.higherkindedj.article2.demo.TraversalDemo
```
- **[CompositionDemo](../src/main/java/org/higherkindedj/article2/demo/CompositionDemo.java)**: Deep path composition for nested updates
```bash
./gradlew :run -PmainClass=org.higherkindedj.article2.demo.CompositionDemo
```
- **[ExpressionPreviewDemo](../src/main/java/org/higherkindedj/article2/demo/ExpressionPreviewDemo.java)**: Preview of the expression language from Article 3
```bash
./gradlew :run -PmainClass=org.higherkindedj.article2.demo.ExpressionPreviewDemo
```

## [Article 3](article-3-ast-basic-optics.md)
- **[ExprDemo](../src/main/java/org/higherkindedj/article3/demo/ExprDemo.java)**: Expression AST and Focus DSL
```bash
./gradlew :run -PmainClass=org.higherkindedj.article3.demo.ExprDemo
```
- **[OptimiserDemo](../src/main/java/org/higherkindedj/article3/demo/OptimiserDemo.java)**: Demonstrates expression optimisation
```bash
./gradlew :run -PmainClass=org.higherkindedj.article3.demo.OptimiserDemo
```

## [Article 4](blog/article-4-traversals-rewrites.md)
- **[Article4Demo](src/main/java/org/higherkindedj/article4/demo/Article4Demo.java)**: Runs all Article 4 demos
```bash
./gradlew :run -PmainClass=org.higherkindedj.article4.demo.Article4Demo
```
- **[TraversalDemo](src/main/java/org/higherkindedj/article4/demo/TraversalDemo.java)**: Traversals for expression trees
```bash
./gradlew :run -PmainClass=org.higherkindedj.article4.demo.TraversalDemo
```
- **[OptimiserDemo](src/main/java/org/higherkindedj/article4/demo/OptimiserDemo.java)**: Expression optimisation using traversals
```bash
./gradlew :run -PmainClass=org.higherkindedj.article4.demo.OptimiserDemo
```

## [Article 5](blog/article-5-effect-polymorphic-optics.md)
- **[Article5Demo](src/main/java/org/higherkindedj/article5/demo/Article5Demo.java)**: Runs all Article 5 demos
```bash
./gradlew :run -PmainClass=org.higherkindedj.article5.demo.Article5Demo
```
- **[EffectPathDemo](src/main/java/org/higherkindedj/article5/demo/EffectPathDemo.java)**: The Effect Path API (MaybePath, EitherPath, ValidationPath)
```bash
./gradlew :run -PmainClass=org.higherkindedj.article5.demo.EffectPathDemo
```
- **[TypeCheckerDemo](src/main/java/org/higherkindedj/article5/demo/TypeCheckerDemo.java)**: Type checking with error accumulation using Validated
```bash
./gradlew :run -PmainClass=org.higherkindedj.article5.demo.TypeCheckerDemo
```
- **[InterpreterDemo](src/main/java/org/higherkindedj/article5/demo/InterpreterDemo.java)**: Expression interpretation using the State monad
```bash
./gradlew :run -PmainClass=org.higherkindedj.article5.demo.InterpreterDemo
```
- **[EffectPolymorphicDemo](src/main/java/org/higherkindedj/article5/demo/EffectPolymorphicDemo.java)**: Effect-polymorphic optics with modifyF
```bash
./gradlew :run -PmainClass=org.higherkindedj.article5.demo.EffectPolymorphicDemo
```
- **[VTaskPathDemo](src/main/java/org/higherkindedj/article5/demo/VTaskPathDemo.java)**: VTaskPath for virtual thread-based concurrency
```bash
./gradlew :run -PmainClass=org.higherkindedj.article5.demo.VTaskPathDemo
```
- **[ParallelTypeCheckerDemo](src/main/java/org/higherkindedj/article5/demo/ParallelTypeCheckerDemo.java)**: Parallel type checking using VTask and Scope
```bash
./gradlew :run -PmainClass=org.higherkindedj.article5.demo.ParallelTypeCheckerDemo
```



