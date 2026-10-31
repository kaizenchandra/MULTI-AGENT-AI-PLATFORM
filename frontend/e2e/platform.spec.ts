import {test,expect} from '@playwright/test';
const user=process.env.LOCAL_USER_TOKEN;const approver=process.env.LOCAL_APPROVER_TOKEN;

async function login(page:import('@playwright/test').Page,token:string){await page.goto('/');await page.getByLabel('Access token').fill(token);await page.getByRole('button',{name:'Connect to workspace'}).click();await expect(page.getByRole('heading',{name:'Workflows',exact:true})).toBeVisible()}
test('knowledge workflow and independent human approval against real backend',async({page,browser})=>{
 test.skip(!user||!approver,'Set local credentials and start backend + Vite first');
 const suffix=Date.now();await login(page,user!);
 await page.getByRole('button',{name:/Knowledge/}).click();
 await page.getByLabel('Document title').fill(`Recovery manual ${suffix}`);await page.getByLabel('Source text').fill('The recovery policy requires daily backups and quarterly restore exercises.');await page.getByRole('button',{name:'Ingest & index'}).click();await expect(page.getByText('Document indexed for full-text and lexical-vector retrieval.')).toBeVisible();
 await page.getByRole('button',{name:/Workflows/}).click();await page.getByLabel('Goal',{exact:true}).fill('recovery policy');await page.getByRole('button',{name:'Run workflow'}).click();await expect(page.getByRole('heading',{name:'Verified result'})).toBeVisible();await expect(page.getByText('The recovery policy requires daily backups and quarterly restore exercises.').first()).toBeVisible();
 await page.getByLabel('Workflow',{exact:true}).selectOption('CREATE_CASE');await page.getByLabel('Goal',{exact:true}).fill(`Review recovery ${suffix}`);await page.getByLabel('Case title').fill(`Recovery review ${suffix}`);await page.getByLabel('Exact proposed description').fill('Review the backup procedure.');await page.getByRole('button',{name:'Run workflow'}).click();await expect(page.getByText('Paused durably.',{exact:false})).toBeVisible();
 const context=await browser.newContext();const approvalPage=await context.newPage();await login(approvalPage,approver!);await approvalPage.getByRole('button',{name:/Approvals/}).click();const card=approvalPage.locator('section').filter({hasText:`Recovery review ${suffix}`});await card.getByRole('button',{name:'Approve exact action'}).click();await expect(page.getByText('Case created:',{exact:false})).toBeVisible();await context.close();
 await page.screenshot({path:'test-results/workflow-completed.png',fullPage:true});
});
