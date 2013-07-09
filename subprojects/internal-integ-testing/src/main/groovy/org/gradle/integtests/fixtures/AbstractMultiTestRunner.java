/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests.fixtures;

import org.junit.internal.builders.IgnoredClassRunner;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.*;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import java.util.*;

/**
 * A base class for those test runners which execute a test multiple times.
 */
public abstract class AbstractMultiTestRunner extends Runner implements Filterable, Sortable {
    protected final Class<?> target;
    private Description description;
    private final List<Execution> executions = new ArrayList<Execution>();

    protected AbstractMultiTestRunner(Class<?> target) {
        this.target = target;
    }

    @Override
    public Description getDescription() {
        initDescription();
        return description;
    }

    @Override
    public void run(RunNotifier notifier) {
        initDescription();
        for (Execution execution : executions) {
            execution.run(notifier);
        }
    }

    public void filter(Filter filter) throws NoTestsRemainException {
        initExecutions();
        for (Execution execution : executions) {
            execution.filter(filter);
        }
        invalidateDescription();
    }

    public void sort(Sorter sorter) {
        initExecutions();
        for (Execution execution : executions) {
            execution.sort(sorter);
        }
        invalidateDescription();
    }

    private void initExecutions() {
        if (executions.isEmpty()) {
            try {
                createExecutions();
                for (Execution execution : executions) {
                    execution.init(target);
                }
            } finally {
                executionsCreated();
            }
        }
    }

    private void initDescription() {
        initExecutions();
        if (description == null) {
            description = Description.createSuiteDescription(target);
            for (Execution execution : executions) {
                execution.addDescriptions(description);
            }
        }
    }

    private void invalidateDescription() {
        description = null;
    }

    protected abstract void createExecutions();

    protected void executionsCreated() {}

    protected void add(Execution execution) {
        executions.add(execution);
    }

    protected static abstract class Execution implements Sortable, Filterable {
        private Runner runner;
        protected Class<?> target;
        private final Map<Description, Description> descriptionTranslations = new HashMap<Description, Description>();

        final void init(Class<?> target) {
            this.target = target;
            if (isEnabled()) {
                try {
                    assertCanExecute();
                    runner = createExecutionRunner();
                } catch (Throwable t) {
                    runner = new ErrorReportingRunner(target, t);
                }
            } else {
                runner = createIgnoredExecutionRunner();
            }
        }

        private Runner createExecutionRunner() throws InitializationError {
            List<? extends Class<?>> targetClasses = loadTargetClasses();
            RunnerBuilder runnerBuilder = new RunnerBuilder() {
                @Override
                public Runner runnerForClass(Class<?> testClass) {
                    try {
                        for (Class<?> candidate = testClass; candidate != null; candidate = candidate.getSuperclass()) {
                            RunWith runWith = candidate.getAnnotation(RunWith.class);
                            if (runWith != null && !AbstractMultiTestRunner.class.isAssignableFrom(runWith.value())) {
                                try {
                                    return (Runner)runWith.value().getConstructors()[0].newInstance(testClass);
                                } catch (Exception e) {
                                    return new ErrorReportingRunner(testClass, e);
                                }
                            }
                        }
                        return new BlockJUnit4ClassRunner(testClass);
                    } catch (InitializationError initializationError) {
                        return new ErrorReportingRunner(testClass, initializationError);
                    }
                }
            };
            return new Suite(runnerBuilder, targetClasses.toArray(new Class<?>[targetClasses.size()])) {
                @Override
                public void run(RunNotifier notifier) {
                    before();
                    try {
                        super.run(notifier);
                    } finally {
                        after();
                    }
                }
            };
        }

        private Runner createIgnoredExecutionRunner() {
            return new IgnoredClassRunner(target) {
                @Override
                public Description getDescription() {
                    return Description.createSuiteDescription(String.format("%s(%s)", getDisplayName(), target.getName()));
                }
            };
        }

        final void addDescriptions(Description parent) {
            if (runner != null) {
                map(runner.getDescription(), parent);
            }
        }

        final void run(final RunNotifier notifier) {
            RunNotifier nested = new RunNotifier();
            nested.addListener(new RunListener() {
                @Override
                public void testStarted(Description description) {
                    Description translated = translateDescription(description);
                    notifier.fireTestStarted(translated);
                }

                @Override
                public void testFailure(Failure failure) {
                    Description translated = translateDescription(failure.getDescription());
                    notifier.fireTestFailure(new Failure(translated, failure.getException()));
                }

                @Override
                public void testAssumptionFailure(Failure failure) {
                    Description translated = translateDescription(failure.getDescription());
                    notifier.fireTestAssumptionFailed(new Failure(translated, failure.getException()));
                }

                @Override
                public void testIgnored(Description description) {
                    Description translated = translateDescription(description);
                    notifier.fireTestIgnored(translated);
                }

                @Override
                public void testFinished(Description description) {
                    Description translated = translateDescription(description);
                    notifier.fireTestFinished(translated);
                }
            });

            runner.run(nested);
        }

        private Description translateDescription(Description description) {
            return descriptionTranslations.containsKey(description) ? descriptionTranslations.get(description) : description;
        }

        public void filter(Filter filter) throws NoTestsRemainException {
            if (runner instanceof Filterable) {
                ((Filterable) runner).filter(filter);
            }
        }

        public void sort(Sorter sorter) {
            if (runner instanceof Sortable) {
                ((Sortable) runner).sort(sorter);
            }
        }

        protected void before() {
        }

        protected void after() {
        }

        private void map(Description source, Description parent) {
            for (Description child : source.getChildren()) {
                Description mappedChild;
                if (child.getMethodName()!= null) {
                    mappedChild = Description.createSuiteDescription(String.format("%s [%s](%s)", child.getMethodName(), getDisplayName(), child.getClassName()));
                    parent.addChild(mappedChild);
                } else {
                    mappedChild = Description.createSuiteDescription(child.getClassName());
                }
                descriptionTranslations.put(child, mappedChild);
                map(child, parent);
            }
        }

        /**
         * Returns a display name for this execution. Used in the JUnit descriptions for test execution.
         */
        protected abstract String getDisplayName();

        /**
         * Returns true if this execution should be executed, false if it should be ignored. Default is true.
         */
        protected boolean isEnabled() {
            return true;
        }

        /**
         * Checks that this execution can be executed, throwing an exception if not.
         */
        protected void assertCanExecute() {
        }

        /**
         * Loads the target classes for this execution. Default is the target class that this runner was constructed with.
         */
        protected List<? extends Class<?>> loadTargetClasses() {
            return Collections.singletonList(target);
        }
    }
}
